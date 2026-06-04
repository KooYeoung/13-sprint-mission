package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserStatusResponse(
      UUID id
      , UUID userId
      , boolean isOnline
      , Instant lastOnlineAt
) {
   public static UserStatusResponse from(UserStatus userStatus) {
      return new UserStatusResponse(
            userStatus.getId()
            , userStatus.getUserId()
            , userStatus.isOnline()
            , userStatus.getUpdatedAt()
            );
   }
}
