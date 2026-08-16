package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.exception.storage.FileNotFoundException;
import com.sprint.mission.discodeit.exception.storage.FileReadFailedException;
import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import com.sprint.mission.discodeit.storage.DownloadResult;
import com.sprint.mission.discodeit.storage.RedirectDownloadResult;
import com.sprint.mission.discodeit.storage.type.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
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
public class S3BinaryContentStorage implements BinaryContentStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties props;
    private final StorageTransactionManager transactionManager;

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
