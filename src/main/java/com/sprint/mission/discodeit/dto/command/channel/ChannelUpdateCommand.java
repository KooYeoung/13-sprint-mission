package com.sprint.mission.discodeit.dto.command.channel;

import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.entity.ChannelType;

public record ChannelUpdateCommand (
        String channelName
        , String channelDescription
        , ChannelType channelType
){
    @Override
    public String channelName() {
        return isPrivate() ? "" : channelName;
    }

    @Override
    public String channelDescription() {
        return isPrivate() ? "" : channelDescription;

    }

    public boolean isPrivate(){
        return ChannelType.PRIVATE.equals(channelType);
    }
}
