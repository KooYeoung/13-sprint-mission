package com.sprint.mission.discodeit.storage.type;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import java.time.Duration;

@ConfigurationProperties(prefix = "discodeit.storage.local")
public record LocalProperties(
        String rootPath,
        @DefaultValue("0ms") Duration simulatedUploadDelay
) {
    public LocalProperties {
        if (simulatedUploadDelay == null || simulatedUploadDelay.isNegative()) {
            throw new IllegalArgumentException("업로드 비교용 지연은 0 이상이어야 합니다.");
        }
    }
}
