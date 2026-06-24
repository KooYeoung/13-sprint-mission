package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserStatusDto(
      UUID id
      , UUID userId
      , boolean isOnline
      , OffsetDateTime lastOnlineAt
) {
   public static UserStatusDto from(UserStatus userStatus) {
      return new UserStatusDto(
            userStatus.getId()
            , userStatus.getUserId()
            , userStatus.isOnline()
            , RequestTimeZoneUtils.toOffsetDateTime(userStatus.getUpdatedAt())
            );
   }

}
