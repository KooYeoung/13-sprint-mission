package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;


public record ChannelCreateRequest(
      String channelName
      , String channelDescription
      , ChannelType channelType
) {

   public Channel toChannel() {
      return new Channel(
            isPrivate() ? "" : channelName
            , isPrivate() ? "" : channelDescription
            , channelType);
   }

   public boolean isPrivate() {
      return channelType.equals(ChannelType.PRIVATE);
   }
}
