package com.sprint.mission.discodeit.dto.request;

import lombok.With;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusCreateRequest(
      UUID userId
      , @With UUID channelId
      , Instant readAt
) {


}
