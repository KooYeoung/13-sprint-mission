package com.sprint.mission.discodeit.dto.request.channel;

import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;
import com.sprint.mission.discodeit.entity.ChannelType;
import lombok.With;

import java.util.UUID;

@With
public record ChannelUpdateRequest(
      String channelName
      , String channelDescription
      , String channelType
) {
    public ChannelUpdateCommand toCommand(){
        return new ChannelUpdateCommand(channelName, channelDescription, ChannelType.getChannelType(channelType));
    }
}
