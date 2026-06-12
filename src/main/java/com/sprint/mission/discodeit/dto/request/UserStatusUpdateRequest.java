package com.sprint.mission.discodeit.dto.request;

import lombok.With;

import java.time.Instant;
import java.util.UUID;

@With
public record UserStatusUpdateRequest(
      UUID id
      , UUID userId
      , Instant lastOnlineAt
) {

}
