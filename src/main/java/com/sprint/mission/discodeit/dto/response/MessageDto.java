package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.aspect.LoggableResult;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MessageDto(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String content,
        UUID channelId,
        UserDto author,
        List<BinaryContentDto> attachments
) implements LoggableResult {

    @Override
    public Map<String, Object> logFields() {
        Map<String, Object> logFields = new LinkedHashMap<>();
        addLogFields(logFields, "messageId", id);
        addLogFields(logFields, "channelId", channelId);
        if (author != null) {
            addLogFields(logFields, "userId", author.id());
        }
        if (attachments != null) {
            addLogFields(logFields, "attachmentCount", attachments.size());
        }
        return logFields;
    }

    private void addLogFields(Map<String, Object> logFields, String key, Object value) {
        logFields.put(key, value);
    }
}