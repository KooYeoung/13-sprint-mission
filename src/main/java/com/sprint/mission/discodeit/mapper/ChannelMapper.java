package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChannelMapper {
    private final UserMapper userMapper;

    public ChannelDto toDto(Channel channel, Instant lastMessageAt, List<ReadStatus> readStatuses) {
        return new ChannelDto(
                channel.getId(),
                channel.getType(),
                channel.getName(),
                channel.getDescription(),
                convertReadStatusToUserDtos(readStatuses),
                RequestTimeZoneUtils.toOffsetDateTime(lastMessageAt)
        );
    }

    public ChannelDto toDto(Channel channel, Instant lastMessageAt) {
        return toDto(channel, lastMessageAt, new ArrayList<>());
    }

    public ChannelDto toDto(Channel channel, List<ReadStatus> readStatuses) {
        return toDto(channel, null, readStatuses);
    }

    public ChannelDto toDto(Channel channel) {
        return toDto(channel ,new ArrayList<>());
    }

    public ChannelDto toDto(ChannelSummary channelSummary, List<ReadStatus> readStatuses) {
        return new ChannelDto(
                channelSummary.id(),
                channelSummary.type(),
                channelSummary.name(),
                channelSummary.description(),
                convertReadStatusToUserDtos(readStatuses),
                RequestTimeZoneUtils.toOffsetDateTime(channelSummary.lastMessageAt())
        );
    }

    private @NonNull List<UserDto> convertReadStatusToUserDtos(List<ReadStatus> readStatuses) {
        return readStatuses
                .stream()
                .map(r -> userMapper.toDto(r.getUser()))
                .toList();
    }
}
