package com.sprint.mission.discodeit.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "discodeit.repository")
public class RepositoryProperties {
    private String type = "jcf";
    private Path fileDirectory = Path.of(".discodeit");
}