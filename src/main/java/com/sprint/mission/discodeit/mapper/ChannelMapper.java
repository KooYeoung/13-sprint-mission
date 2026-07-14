package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ReadStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Mapper(
        config = MapStructConfig.class,
        uses = {UserMapper.class, DateTimeMapper.class, ChannelParticipantMapper.class}
)
public interface ChannelMapper {

    @Mapping(source = "channel.id", target = "id")
    @Mapping(source = "channel.type", target = "type")
    @Mapping(source = "channel.name", target = "name")
    @Mapping(source = "channel.description", target = "description")
    @Mapping(source = "readStatuses", target = "participants", qualifiedByName = "readStatusesToParticipants")
    @Mapping(source = "lastMessageAt", target = "lastMessageAt", qualifiedByName = "toOffsetDateTime")
    ChannelDto toDto(Channel channel, Instant lastMessageAt, List<ReadStatus> readStatuses);

    @Mapping(source = "channelSummary.id", target = "id")
    @Mapping(source = "channelSummary.type", target = "type")
    @Mapping(source = "channelSummary.name", target = "name")
    @Mapping(source = "channelSummary.description", target = "description")
    @Mapping(source = "readStatuses", target = "participants", qualifiedByName = "readStatusesToParticipants")
    @Mapping(source = "channelSummary.lastMessageAt", target = "lastMessageAt", qualifiedByName = "toOffsetDateTime")
    ChannelDto toDto(ChannelSummary channelSummary, List<ReadStatus> readStatuses);

    default ChannelDto toDto(Channel channel, List<ReadStatus> readStatuses) {
        return toDto(channel, null, readStatuses);
    }

    default ChannelDto toDto(Channel channel) {
        return toDto(channel, new ArrayList<>());
    }

}
