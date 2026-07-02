package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
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
    public static ChannelDto from(Channel channel) {
        return new ChannelDto(
                channel.getId(),
                channel.getType(),
                channel.getName(),
                channel.getDescription(),
                convertReadStatusToUserDtos(channel),
                null
        );
    }


    public static ChannelDto from(Channel channel, Instant lastMessageAt) {
        return new ChannelDto(
                channel.getId(),
                channel.getType(),
                channel.getName(),
                channel.getDescription(),
                convertReadStatusToUserDtos(channel),
                RequestTimeZoneUtils.toOffsetDateTime(lastMessageAt)
        );
    }

    public boolean isPrivate() {
        return ChannelType.PRIVATE.equals(type);
    }

    private static @NonNull List<UserDto> convertReadStatusToUserDtos(Channel channel) {
        return channel.getReadStatusList()
                .stream()
                .map(r -> UserDto.from(r.getUser()))
                .toList();
    }

}
