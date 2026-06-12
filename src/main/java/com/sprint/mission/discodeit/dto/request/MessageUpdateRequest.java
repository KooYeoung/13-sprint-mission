package com.sprint.mission.discodeit.dto.request;

import lombok.With;

import java.util.UUID;

@With
public record MessageUpdateRequest(
      UUID messageId
      , String content
      , UUID channelId
      , UUID userId
) {
}
