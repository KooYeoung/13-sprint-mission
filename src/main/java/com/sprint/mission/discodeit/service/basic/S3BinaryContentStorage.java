package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.exception.storage.FileNotFoundException;
import com.sprint.mission.discodeit.exception.storage.FileReadFailedException;
import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import com.sprint.mission.discodeit.storage.DownloadResult;
import com.sprint.mission.discodeit.storage.RedirectDownloadResult;
import com.sprint.mission.discodeit.storage.type.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "discodeit.storage.type",
        havingValue = "s3"
)
@RequiredArgsConstructor
@Slf4j
public class S3BinaryContentStorage implements BinaryContentStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties props;
    private final StorageTransactionManager transactionManager;
    private final UserReader userReader;
    private final NotificationService notificationService;

    @Retryable(
            retryFor = FileSaveFailedException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Override
    public UUID put(UUID fileId, BinaryContentUpload upload) {

        String key = fileId.toString();

        try (InputStream inputStream = upload.inputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .contentType(upload.contentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, upload.contentLength()));

        } catch (S3Exception | SdkClientException | IOException e) {
            throw new FileSaveFailedException(fileId, "S3 파일 업로드 중 오류가 발생했습니다.", e);
        }

        transactionManager.afterRollback(
                "S3 파일 업로드 롤백 정리. key=" + key,
                () -> s3Client.deleteObject(b -> b.bucket(props.bucket()).key(key))
        );

        return fileId;
    }

    @Recover
    public UUID recover(FileSaveFailedException failure, UUID fileId, BinaryContentUpload upload) {
        String requestId = MDC.get("requestId");
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(failure);
        log.error("S3 업로드 재시도를 모두 실패했습니다. operation=S3BinaryContentStorage.put, requestId={}, binaryContentId={}",
                requestId, fileId, failure);

        try {
            List<UUID> adminIds = userReader.findUserIdsByRole(Role.ADMIN);
            if (adminIds.isEmpty()) {
                log.warn("업로드 실패 알림을 받을 관리자가 없습니다. binaryContentId={}", fileId);
            } else {
                String content = """
                        작업: S3BinaryContentStorage.put
                        RequestId: %s
                        BinaryContentId: %s
                        Error: %s
                        """.formatted(requestId == null ? "없음" : requestId, fileId, cause.toString());
                notificationService.createAll(new NotificationCreateCommand(
                        adminIds, "S3 파일 업로드에 실패했습니다.", content));
                log.info("업로드 실패 관리자 알림을 생성했습니다. binaryContentId={}, recipientCount={}",
                        fileId, adminIds.size());
            }
        } catch (RuntimeException notificationFailure) {
            if (failure != notificationFailure) failure.addSuppressed(notificationFailure);
            log.error("업로드 실패 관리자 알림 생성에 실패했습니다. binaryContentId={}", fileId, notificationFailure);
        }

        // 정상 반환하면 Processor가 SUCCESS로 처리하므로 원래 실패를 전달한다.
        throw failure;
    }

    @Override
    public InputStream get(UUID fileId) {
        String key = fileId.toString();

        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException(fileId, "S3에서 파일을 찾을 수 없습니다.", e);
        } catch (S3Exception | SdkClientException e) {
            throw new FileReadFailedException(fileId, "S3 파일을 읽는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void delete(UUID fileId) {
        String key = fileId.toString();

        transactionManager.afterCommit(
                "S3 파일 커밋 후 삭제. key=" + key,
                () -> s3Client.deleteObject(b -> b.bucket(props.bucket()).key(key))
        );
    }

    @Override
    public DownloadResult download(BinaryContentDto binaryContentDto) {

        String key = binaryContentDto.id().toString();
        String storedName = binaryContentDto.fileName();

        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(props.presignedUrlExpiration()))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(key)
                        .responseContentDisposition(getContentDisposition(storedName))
                        .build())
                .build();

        URI location;
        try {
            location = s3Presigner.presignGetObject(request).url().toURI();
        } catch (S3Exception | SdkClientException | URISyntaxException e) {
            throw new FileReadFailedException(binaryContentDto.id(), "S3 다운로드 URL 생성 중 오류가 발생했습니다.", e);
        }

        return new RedirectDownloadResult(location);
    }

    @Override
    public void deleteAll(List<UUID> binaryContentIds) {

        binaryContentIds.forEach(this::delete);
    }

    private String getContentDisposition(String storedName) {
        return ContentDisposition.attachment().filename(storedName, StandardCharsets.UTF_8).build().toString();
    }
}
