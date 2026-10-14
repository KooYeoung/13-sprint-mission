package com.sprint.mission.discodeit.integration;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.events.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentStatusUpdater;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import com.sprint.mission.discodeit.service.basic.BinaryContentService;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.sprint.mission.discodeit.support.AsyncTestSupport.awaitIdle;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:binary-upload-transactions;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "discodeit.storage.type=local"
})
@ActiveProfiles("test")
@RecordApplicationEvents
@DisplayName("바이너리 파일 업로드 트랜잭션 통합 테스트")
class BinaryContentUploadTransactionIntegrationTest {
    @Autowired BinaryContentService service;
    @Autowired BinaryContentStatusUpdater statusUpdater;
    @Autowired BinaryContentRepository repository;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean BinaryContentStorage storage;
    @Autowired ThreadPoolTaskExecutor fileUploadExecutor;
    @Autowired ApplicationEvents events;

    private TransactionTemplate transaction;
    private final List<UUID> createdIds = new ArrayList<>();

    @BeforeEach
    void setUpTransaction() {
        transaction = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void deleteCommittedMetadata() {
        awaitIdle(fileUploadExecutor);
        transaction.executeWithoutResult(status -> repository.deleteAllById(createdIds));
    }

    @Test
    @DisplayName("커밋 전에는 업로드하지 않고 커밋 후 업로드 결과를 별도 트랜잭션에 저장한다")
    void create_uploadsOnlyAfterCommitAndPersistsSuccess() {
        given(storage.put(any(UUID.class), any(BinaryContentUpload.class))).willAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            // 업로드 시점에는 메타데이터가 이미 다른 트랜잭션에서 조회 가능해야 한다.
            TransactionTemplate independent = new TransactionTemplate(transactionManager);
            independent.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
            BinaryContentStatus committedStatus = independent.execute(
                    status -> repository.findById(id).orElseThrow().getStatus());
            assertThat(committedStatus).isEqualTo(BinaryContentStatus.PROCESSING);
            try (InputStream stream = invocation.<BinaryContentUpload>getArgument(1).inputStream()) {
                assertThat(stream.readAllBytes()).containsExactly(1, 2, 3);
            }
            return id;
        });

        UUID id = transaction.execute(status -> {
            BinaryContent content = createFile();
            assertThat(repository.findById(content.getId()).orElseThrow().getStatus())
                    .isEqualTo(BinaryContentStatus.PROCESSING);
            verifyNoInteractions(storage);
            return content.getId();
        });

        await().untilAsserted(() -> assertThat(reload(id).getStatus()).isEqualTo(BinaryContentStatus.SUCCESS));
        verify(storage).put(eq(id), any(BinaryContentUpload.class));
        BinaryContent saved = reload(id);
        assertThat(saved.getStatus()).isEqualTo(BinaryContentStatus.SUCCESS);
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("커밋 후 업로드가 실패하면 메타데이터를 유지하고 실패 상태를 저장한다")
    void create_preservesCommittedMetadataAndPersistsFail() {
        given(storage.put(any(UUID.class), any(BinaryContentUpload.class))).willAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            invocation.<BinaryContentUpload>getArgument(1).inputStream().close();
            throw new FileSaveFailedException(id, "테스트 저장소 업로드 실패", new IOException("저장소 오류"));
        });

        // AFTER_COMMIT 후처리 예외는 이미 완료된 메타데이터 커밋을 되돌리지 않는다.
        UUID id = transaction.execute(status -> createFile().getId());

        await().untilAsserted(() -> assertThat(reload(id).getStatus()).isEqualTo(BinaryContentStatus.FAIL));
        verify(storage).put(eq(id), any(BinaryContentUpload.class));
    }

    @Test
    @DisplayName("메인 트랜잭션을 롤백하면 업로드 없이 임시 파일을 삭제한다")
    void create_deletesTemporaryFileWithoutUploadingAfterRollback() {
        TrackingInputStream stream = new TrackingInputStream();
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1}) {
            @Override
            public InputStream getInputStream() {
                return stream;
            }
        };
        AtomicReference<UUID> id = new AtomicReference<>();
        AtomicReference<Path> temporaryFile = new AtomicReference<>();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            id.set(service.create(file).orElseThrow().getId());
            createdIds.add(id.get());
            temporaryFile.set(events.stream(BinaryContentCreatedEvent.class).findFirst().orElseThrow()
                    .contentUpload().temporaryFile());
            assertThat(temporaryFile.get()).exists();
            assertThat(stream.closed).isTrue();
            throw new IllegalStateException("메인 작업 실패");
        })).isInstanceOf(IllegalStateException.class).hasMessage("메인 작업 실패");

        assertThat(repository.findById(id.get())).isEmpty();
        assertThat(stream.closed).isTrue();
        assertThat(temporaryFile.get()).doesNotExist();
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("별도 트랜잭션의 상태 변경은 호출한 외부 트랜잭션이 롤백되어도 유지된다")
    void updateStatus_commitsIndependentlyOfOuterRollback() {
        UUID id = transaction.execute(status -> {
            BinaryContent content = repository.save(BinaryContent.createProcessing("file.txt", "text/plain", 1L));
            createdIds.add(content.getId());
            return content.getId();
        });

        transaction.executeWithoutResult(status -> {
            statusUpdater.updateStatus(id, BinaryContentStatus.SUCCESS);
            status.setRollbackOnly();
        });

        assertThat(reload(id).getStatus()).isEqualTo(BinaryContentStatus.SUCCESS);
        verifyNoInteractions(storage);
    }

    private BinaryContent createFile() {
        BinaryContent content = service.create(new MockMultipartFile(
                "file", "file.txt", "text/plain", new byte[]{1, 2, 3})).orElseThrow();
        createdIds.add(content.getId());
        return content;
    }

    private BinaryContent reload(UUID id) {
        return transaction.execute(status -> repository.findById(id).orElseThrow());
    }

    private static class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        TrackingInputStream() {
            super(new byte[]{1});
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
