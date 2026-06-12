package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserStatusDto(
      UUID id
      , UUID userId
      , boolean isOnline
      , Instant lastOnlineAt
) {
   public static UserStatusDto from(UserStatus userStatus) {
      return new UserStatusDto(
            userStatus.getId()
            , userStatus.getUserId()
            , userStatus.isOnline()
            , userStatus.getUpdatedAt()
            );
   }

   public static UserStatusDto from(UserStatusUpdateRequest request){

       return new UserStatusDto(
               request.id()
               ,request.userId()
               ,false
               ,request.lastOnlineAt()
       );

   }
}
