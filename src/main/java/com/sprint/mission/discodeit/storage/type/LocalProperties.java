package com.sprint.mission.discodeit.storage.type;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discodeit.storage.local")
public record LocalProperties(
        String rootPath
) {
}
