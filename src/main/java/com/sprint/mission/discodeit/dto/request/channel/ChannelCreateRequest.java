package com.sprint.mission.discodeit.dto.request.channel;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreateCommand;
import com.sprint.mission.discodeit.entity.ChannelType;

public record ChannelCreateRequest(
      String channelName
      , String channelDescription
      , String channelType
) {

    public ChannelCreateCommand toCommand(){
        return new ChannelCreateCommand(channelName, channelDescription, ChannelType.getChannelType(channelType));
    }

}
