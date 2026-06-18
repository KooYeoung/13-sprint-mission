package com.sprint.mission.discodeit.dto.request.userStatus;

import java.time.Instant;
import java.util.UUID;

public record UserStatusCreateRequest(
      UUID userId
      , Instant createdAt
) {

}
