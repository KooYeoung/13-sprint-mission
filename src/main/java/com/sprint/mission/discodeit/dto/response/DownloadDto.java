package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.aspect.LoggableResult;
import org.springframework.core.io.Resource;

import java.util.LinkedHashMap;
import java.util.Map;

public record DownloadDto (
        BinaryContentDto binaryContent,
        Resource resource
) implements LoggableResult {


    @Override
    public Map<String, Object> logFields() {
        Map<String, Object> logFields = new LinkedHashMap<>();
        addLogFields(logFields, "binaryContentId", binaryContent.id());
        return logFields;
    }

    private void addLogFields(Map<String, Object> logFields, String key, Object value) {
        logFields.put(key, value);
    }
}
