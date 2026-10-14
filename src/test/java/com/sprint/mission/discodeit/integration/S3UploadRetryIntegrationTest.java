package com.sprint.mission.discodeit.integration;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserRoleUpdateCommand;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.events.BinaryContentCreatedEvent;
import com.sprint.mission.discodeit.exception.AsyncExceptionHandler;
import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.basic.BinaryContentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.sprint.mission.discodeit.support.AsyncTestSupport.awaitIdle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:s3-retry;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "discodeit.storage.type=s3",
        "discodeit.storage.s3.region=ap-northeast-2",
        "discodeit.storage.s3.bucket=retry-test",
        "discodeit.storage.s3.endpoint=http://localhost:9999",
        "discodeit.async.enabled=true"
})
@ActiveProfiles("test")
@RecordApplicationEvents
class S3UploadRetryIntegrationTest {
    @Autowired BinaryContentService service;
    @Autowired BinaryContentRepository binaries;
    @Autowired NotificationRepository notifications;
    @Autowired UserRepository users;
    @Autowired ThreadPoolTaskExecutor fileUploadExecutor;
    @Autowired ThreadPoolTaskExecutor notificationExecutor;
    @Autowired ApplicationEvents events;
    @MockitoBean S3Client s3;
    @MockitoBean S3Presigner presigner;
    @MockitoBean AsyncExceptionHandler exceptionHandler;
    // 실제 알림 저장을 실행하며 호출 횟수와 알림 저장 실패 경로도 검증한다.
    @MockitoSpyBean NotificationService notificationService;

    private final List<UUID> createdUserIds = new ArrayList<>();
    private List<UUID> adminIds;

    @BeforeEach
    void createRecipients() {
        // 시작 시 관리자 권한 변경 알림이 완료된 후 업로드 실패 알림만 측정한다.
        awaitIdle(fileUploadExecutor, notificationExecutor);
        notifications.deleteAllInBatch();
        clearInvocations(notificationService);
        for (Role role : Role.values()) {
            String name = "retry-" + UUID.randomUUID();
            User user = new User(new UserCreateCommand(name, "password", name + "@test.local"), null);
            user.updateRole(new UserRoleUpdateCommand(role));
            createdUserIds.add(users.saveAndFlush(user).getId());
        }
        adminIds = users.findUserIdsByRole(Role.ADMIN);
    }

    @AfterEach
    void cleanup() {
        awaitIdle(fileUploadExecutor, notificationExecutor);
        notifications.deleteAllInBatch();
        binaries.deleteAllInBatch();
        users.deleteAllById(createdUserIds);
        MDC.clear();
    }

    @Test
    void retriesWholeFileAndPersistsSuccessWithoutFailureNotification() throws Exception {
        byte[] bytes = new byte[256 * 1024];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i % 251);
        List<byte[]> attempts = new CopyOnWriteArrayList<>();
        given(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).willAnswer(invocation -> {
            PutObjectRequest request = invocation.getArgument(0);
            assertThat(binaries.findById(UUID.fromString(request.key())).orElseThrow().getStatus())
                    .isEqualTo(BinaryContentStatus.PROCESSING);
            RequestBody body = invocation.getArgument(1);
            try (var input = body.contentStreamProvider().newStream()) {
                attempts.add(input.readAllBytes());
            }
            if (attempts.size() < 3) throw s3Failure();
            return PutObjectResponse.builder().build();
        });

        UUID id = service.create(new MockMultipartFile("file", "data.bin", "application/octet-stream", bytes))
                .orElseThrow().getId();
        awaitIdle(fileUploadExecutor);

        assertThat(attempts).hasSize(3).allSatisfy(attempt -> assertThat(attempt).isEqualTo(bytes));
        assertThat(binaries.findById(id).orElseThrow().getStatus()).isEqualTo(BinaryContentStatus.SUCCESS);
        verify(s3, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(notificationService, never()).createAll(any());
        assertThat(notifications.count()).isZero();
        verifyNoInteractions(exceptionHandler);
        assertTemporaryFileDeleted();
    }

    @Test
    void exhaustedRetriesNotifyOnlyAdministratorsOnceAndPersistFail() {
        S3Exception failure = s3Failure();
        given(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).willThrow(failure);
        MDC.put("requestId", "retry-request-id");

        UUID id = createFile();
        MDC.clear();
        awaitIdle(fileUploadExecutor);

        verify(s3, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(notificationService).createAll(any(NotificationCreateCommand.class));
        assertThat(binaries.findById(id).orElseThrow().getStatus()).isEqualTo(BinaryContentStatus.FAIL);
        List<Notification> saved = notifications.findAll();
        assertThat(saved).extracting(Notification::getReceiverId).containsExactlyInAnyOrderElementsOf(adminIds);
        assertThat(saved).allSatisfy(notification -> {
            assertThat(notification.getTitle()).isEqualTo("S3 파일 업로드에 실패했습니다.");
            assertThat(notification.getContent()).contains("S3BinaryContentStorage.put", "retry-request-id", id.toString(), "ServiceUnavailable");
            assertThat(notification.getContent().codePointCount(0, notification.getContent().length())).isLessThanOrEqualTo(255);
        });
        assertThat(capturedFailure()).isInstanceOf(FileSaveFailedException.class).hasCause(failure);
        assertTemporaryFileDeleted();
    }

    @Test
    void notificationFailureDoesNotPreventFailStatusOrMaskStorageFailure() {
        S3Exception failure = s3Failure();
        var notificationFailure = new IllegalStateException("알림 저장 실패");
        given(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).willThrow(failure);
        doThrow(notificationFailure).when(notificationService).createAll(any());

        UUID id = createFile();
        awaitIdle(fileUploadExecutor);

        assertThat(binaries.findById(id).orElseThrow().getStatus()).isEqualTo(BinaryContentStatus.FAIL);
        Throwable captured = capturedFailure();
        assertThat(captured).isInstanceOf(FileSaveFailedException.class).hasCause(failure);
        assertThat(captured.getSuppressed()).containsExactly(notificationFailure);
        assertThat(notifications.count()).isZero();
        verify(s3, times(3)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(notificationService).createAll(any(NotificationCreateCommand.class));
        assertTemporaryFileDeleted();
    }

    private UUID createFile() {
        return service.create(new MockMultipartFile("file", "data.txt", "text/plain", new byte[]{1, 2, 3}))
                .orElseThrow().getId();
    }

    private S3Exception s3Failure() {
        S3Exception.Builder builder = S3Exception.builder();
        builder.statusCode(503);
        builder.message("ServiceUnavailable " + "상세 오류 ".repeat(100));
        return (S3Exception) builder.build();
    }

    private Throwable capturedFailure() {
        var captured = ArgumentCaptor.forClass(Throwable.class);
        verify(exceptionHandler).handleUncaughtException(captured.capture(), any(), any());
        return captured.getValue();
    }

    private void assertTemporaryFileDeleted() {
        var event = events.stream(BinaryContentCreatedEvent.class).findFirst().orElseThrow();
        assertThat(event.contentUpload().temporaryFile()).doesNotExist();
    }
}
