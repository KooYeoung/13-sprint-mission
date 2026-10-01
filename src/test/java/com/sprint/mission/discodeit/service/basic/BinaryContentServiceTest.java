package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.events.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.exception.storage.FileReadFailedException;
import com.sprint.mission.discodeit.mapper.BinaryContentMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("바이너리 파일 생성 서비스 단위 테스트")
class BinaryContentServiceTest {
    @Mock BinaryContentRepository repository;
    @Mock BinaryContentStorage storage;
    @Mock BinaryContentMapper mapper;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks BinaryContentService service;

    @Test
    @DisplayName("메타데이터와 업로드 내용을 담은 이벤트를 한 번 발행하고 저장소는 호출하지 않는다")
    void create_publishesUploadEventWithoutWritingStorage() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1, 2, 3});
        UUID id = UUID.randomUUID();
        given(repository.save(any(BinaryContent.class))).willAnswer(invocation -> {
            BinaryContent content = invocation.getArgument(0);
            // 이벤트 인자 검증을 위한 ID이며, DB 저장 성공을 검증하는 설정은 아니다.
            ReflectionTestUtils.setField(content, "id", id);
            return content;
        });

        BinaryContent result = service.create(file).orElseThrow();

        ArgumentCaptor<BinaryContent> saved = ArgumentCaptor.forClass(BinaryContent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue()).isSameAs(result);
        assertThat(result.getStatus()).isEqualTo(BinaryContentStatus.PROCESSING);
        assertThat(result.getOriginalFileName()).isEqualTo("file.txt");
        assertThat(result.getContentType()).isEqualTo("text/plain");
        assertThat(result.getSize()).isEqualTo(3L);
        ArgumentCaptor<BinaryContentCreatedEvent> published = ArgumentCaptor.forClass(BinaryContentCreatedEvent.class);
        verify(eventPublisher).publishEvent(published.capture());
        BinaryContentCreatedEvent event = published.getValue();
        assertThat(event.binaryContentId()).isEqualTo(id);
        assertThat(event.contentUpload().contentType()).isEqualTo("text/plain");
        assertThat(event.contentUpload().contentLength()).isEqualTo(file.getSize());
        try (InputStream stream = event.contentUpload().inputStream()) {
            assertThat(stream.readAllBytes()).containsExactly(file.getBytes());
        }
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("파일이 없거나 비어 있으면 메타데이터 저장과 이벤트 발행을 생략한다")
    void create_skipsMissingAndEmptyFiles() {
        assertThat(service.create(null)).isEmpty();
        assertThat(service.create(new MockMultipartFile("file", new byte[0]))).isEmpty();

        verifyNoInteractions(repository, eventPublisher, storage);
    }

    @Test
    @DisplayName("메타데이터 저장에 실패하면 이벤트를 발행하지 않는다")
    void create_doesNotPublishWhenMetadataSaveFails() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("메타데이터 저장 실패");
        given(repository.save(any(BinaryContent.class))).willThrow(failure);

        assertThatThrownBy(() -> service.create(new MockMultipartFile("file", new byte[]{1})))
                .isSameAs(failure);

        verifyNoInteractions(eventPublisher, storage);
    }

    @Test
    @DisplayName("파일 스트림을 열 수 없으면 읽기 실패 예외를 던지고 이벤트를 발행하지 않는다")
    void create_doesNotPublishWhenStreamCannotBeOpened() {
        MockMultipartFile file = new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1}) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("스트림 열기 실패");
            }
        };
        given(repository.save(any(BinaryContent.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.create(file)).isInstanceOf(FileReadFailedException.class);

        verifyNoInteractions(eventPublisher, storage);
    }
}
