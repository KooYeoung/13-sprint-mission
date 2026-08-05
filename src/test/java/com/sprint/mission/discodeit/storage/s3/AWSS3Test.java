package com.sprint.mission.discodeit.storage.s3;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.sprint.mission.discodeit.storage.type.S3Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AWSS3Test {

    private static final String BUCKET = envOrDefault("AWS_S3_BUCKET", "discodeit-test-bucket");
    private static final String REGION = envOrDefault("AWS_S3_REGION", "ap-northeast-2");
    private static final int PRESIGNED_URL_EXPIRATION = intEnvOrDefault("AWS_S3_PRESIGNED_URL_EXPIRATION", 600);

    static S3MockContainer s3Mock;
    static S3Client s3Client;
    static S3Presigner s3Presigner;
    static S3Properties s3Properties;

    @BeforeAll
    static void setUp() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is required for S3Mock tests.");

        s3Mock = new S3MockContainer("4.11.0")
                .withInitialBuckets(BUCKET);
        s3Mock.start();

        s3Properties = new S3Properties(
                REGION,
                BUCKET,
                s3Mock.getHttpEndpoint(),
                PRESIGNED_URL_EXPIRATION
        );

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(s3Properties.endpoint()))
                .region(Region.of(s3Properties.region()))
                .forcePathStyle(true)
                .credentialsProvider(testCredentials())
                .build();

        s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(s3Properties.endpoint()))
                .region(Region.of(s3Properties.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .credentialsProvider(testCredentials())
                .build();
    }

    @AfterAll
    static void tearDown() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
        if (s3Mock != null) {
            s3Mock.stop();
        }
    }

    @Test
    @DisplayName("S3 SDK upload")
    void upload_putsObjectToS3() {
        String key = uniqueKey("upload");
        byte[] bytes = "s3-upload-content".getBytes();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .contentType("text/plain")
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        var headObject = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build());

        assertThat(headObject.contentLength()).isEqualTo(bytes.length);
        assertThat(headObject.contentType()).isEqualTo("text/plain");
    }

    @Test
    @DisplayName("S3 SDK download")
    void download_readsUploadedObjectFromS3() throws Exception {
        String key = uniqueKey("download");
        byte[] bytes = "s3-download-content".getBytes();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        try (ResponseInputStream<GetObjectResponse> object = s3Client.getObject(GetObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build())) {
            assertThat(object.readAllBytes()).isEqualTo(bytes);
        }
    }

    @Test
    @DisplayName("S3 SDK presigned URL")
    void presignedUrl_createsGetObjectUrl() {
        String key = uniqueKey("presigned");
        byte[] bytes = "s3-presigned-content".getBytes();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .build(),
                RequestBody.fromBytes(bytes)
        );

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(s3Properties.presignedUrlExpiration()))
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(s3Properties.bucket())
                                .key(key)
                                .build())
                        .build()
        );

        String url = presignedRequest.url().toString();
        assertThat(url).contains(s3Properties.bucket());
        assertThat(url).contains(key);
        assertThat(url).contains("X-Amz-Signature");
    }

    private static StaticCredentialsProvider testCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    }

    private static String uniqueKey(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private static String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int intEnvOrDefault(String key, int defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }
}
