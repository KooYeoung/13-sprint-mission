package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;


public record ChannelCreateRequest(
      String channelName
      , String channelDescription
      , String channelType
) {

}
