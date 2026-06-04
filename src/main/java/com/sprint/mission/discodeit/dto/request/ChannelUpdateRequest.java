package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.ChannelType;

import java.util.UUID;

public record ChannelUpdateRequest(
      UUID channelId
      , String channelName
      , String channelDescription
      , ChannelType channelType
) {

   public boolean isPrivate() {
      return channelType.equals(ChannelType.PRIVATE);
   }
}
