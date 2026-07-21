package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.aspect.LoggableResult;
import com.sprint.mission.discodeit.entity.ChannelType;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ChannelDto(
        UUID id,
        ChannelType type,
        String name,
        String description,
        List<UserDto> participants,
        OffsetDateTime lastMessageAt
) implements LoggableResult {
    public boolean isPrivate() {
        return ChannelType.PRIVATE.equals(type);
    }

    @Override
    public Map<String, Object> logFields() {
        Map<String, Object> logFields = new LinkedHashMap<>();
        addLogFields(logFields, "channelId", id);
        addLogFields(logFields, "channelType", type);
        if (participants != null) {
            addLogFields(logFields, "participantCount", participants.size());
        }
        return logFields;
    }

    private void addLogFields(Map<String, Object> logFields, String key, Object value) {
        logFields.put(key, value);
    }
}
