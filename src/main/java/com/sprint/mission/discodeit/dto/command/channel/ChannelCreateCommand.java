package com.sprint.mission.discodeit.dto.command.channel;

import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.entity.ChannelType;

public record ChannelCreateCommand (
        String channelName
        , String channelDescription
        , ChannelType channelType
){

    public static ChannelCreateCommand from(ChannelDto dto){
        return new ChannelCreateCommand(
                dto.isPrivate() ? "" : dto.channelName()
                , dto.isPrivate() ? "" : dto.description()
                , dto.channelType()
        );
    }

}
