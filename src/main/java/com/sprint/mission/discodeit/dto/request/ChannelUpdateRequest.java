package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.ChannelType;
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
