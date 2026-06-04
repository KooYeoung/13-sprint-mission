package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserStatusCreateRequest(
      UUID userId
      , Instant createdAt
) {
   public UserStatus toUserStatus() {

      return new UserStatus(
             createdAt
            , userId);
   }
}
