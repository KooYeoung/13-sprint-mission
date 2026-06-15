package com.sprint.mission.discodeit.dto.request;

public record ChannelCreateRequest(
      String channelName
      , String channelDescription
      , String channelType
) {

}
