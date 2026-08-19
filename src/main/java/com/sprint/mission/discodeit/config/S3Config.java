package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.storage.type.S3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "s3")
@EnableConfigurationProperties(S3Properties.class)
@Slf4j
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties props) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(props.region()));

        return initializeS3Client(props, builder).build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties props) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(props.region()));

        return initializeS3Presigner(props, builder).build();
    }

    private S3Presigner.Builder initializeS3Presigner(S3Properties props, S3Presigner.Builder builder) {
        if (isEndpointPresent(props)) {
            return builder
                    .endpointOverride(URI.create(props.endpoint())) // test url로 endpoint를 설정
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));

        }

        return builder.serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .build())
                .credentialsProvider(DefaultCredentialsProvider.builder().build());
    }

    private S3ClientBuilder initializeS3Client(S3Properties props, S3ClientBuilder builder) {
        if (isEndpointPresent(props)) {
            // endpoint 에 값이 있다면 테스트환경
            S3ClientBuilder s3ClientBuilder = builder.endpointOverride(URI.create(props.endpoint())) // test url로 endpoint를 설정
                    .forcePathStyle(true)// 가상 호스팅 방식이 아닌 경로 방식의 url 사용을 강제한다.
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
            // 더미 자격 증명 설정, 실제로 AWS에 접속하는 것이 아니기 때문에 아무 값이나 넣어서 형식만 갖추겠다. (아예 없으면 에러 발생);
            log.info("S3Client -  로컬 Mock 사용: {}", props.endpoint());
            return s3ClientBuilder;
        }

        S3ClientBuilder s3ClientBuilder = builder.forcePathStyle(false)
                // DefaultCredentialProvider는 자격증명을 정해진 순서대로 찾아보는 체인 객체입니다.
                // 1. 환경변수로 전달된 값이 있는가? (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
                // 2. 자바 시스템 프로퍼티
                // 3. aws-cli를 통해 설정된 프로파일이 존재 하는지
                // 4. ECS 태스크 역할 / EC2 인스턴스 프로파일
                .credentialsProvider(DefaultCredentialsProvider.builder().build());
        log.info("S3Client - 실제 AWS(region={})", props.region());

        return s3ClientBuilder;
    }

    private boolean isEndpointPresent(S3Properties props) {
        return StringUtils.hasText(props.endpoint());
    }

}
