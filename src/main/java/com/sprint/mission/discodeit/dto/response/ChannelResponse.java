package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import lombok.With;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ChannelResponse(
      UUID id
      , String channelName
      , String description
      , ChannelType channelType
      , @With Instant lastMessageAt
      , @With List<UUID> userIds
) {

   public static ChannelResponse from(Channel channel) {
      return new ChannelResponse(channel.getId()
            , channel.getChannelName()
            , channel.getDescription()
            , channel.getChannelType()
            , null
            ,  new ArrayList<>());
   }
}
