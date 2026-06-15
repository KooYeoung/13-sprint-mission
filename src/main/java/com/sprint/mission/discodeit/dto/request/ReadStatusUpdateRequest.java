package com.sprint.mission.discodeit.dto.request;

import lombok.With;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusUpdateRequest (
      @With UUID id
      , Instant readAt
      , UUID userId
      , @With UUID channelId
      ){

}
