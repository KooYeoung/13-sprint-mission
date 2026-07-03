package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.ChannelType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChannelDto(
        UUID id,
        ChannelType type,
        String name,
        String description,
        List<UserDto> participants,
        OffsetDateTime lastMessageAt
) {
    public boolean isPrivate() {
        return ChannelType.PRIVATE.equals(type);
    }

}
