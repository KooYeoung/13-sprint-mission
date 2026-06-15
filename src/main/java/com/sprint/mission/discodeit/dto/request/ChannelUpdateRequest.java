package com.sprint.mission.discodeit.dto.request;

import lombok.With;

import java.util.UUID;

@With
public record ChannelUpdateRequest(
      UUID channelId
      , String channelName
      , String channelDescription
      , String channelType
) {

}
