package com.sprint.mission.discodeit.dto.command.channel;

import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.entity.ChannelType;

public record ChannelUpdateCommand (
        String channelName
        , String channelDescription
        , ChannelType channelType
){
    public static ChannelUpdateCommand from(ChannelDto dto){
        return new ChannelUpdateCommand(
                dto.isPrivate() ? "" : dto.channelName()
                , dto.isPrivate() ? "" : dto.description()
                , dto.channelType()
        );
    }
}
