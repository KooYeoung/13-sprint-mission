package com.sprint.mission.discodeit.storage.type;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discodeit.storage.s3")
public record S3Properties(
        String region,
        String bucket,
        String endpoint,
        Integer presignedUrlExpiration
) {
}
