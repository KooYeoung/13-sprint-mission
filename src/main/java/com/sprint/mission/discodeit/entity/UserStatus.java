package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public class UserStatus extends UpdatableEntity {

   private final UUID userId;
   public UserStatus(UserStatusCreateCommand command) {
      super(command.createdAt());
      this.userId = command.userId();
   }

   public UserStatus updateInfo(UserStatusUpdateCommand command){
      return new UserStatus(
              getId()
              , getCreatedAt()
              , command.updateAt()
              , userId
      );
   }

   private UserStatus(UUID id,
                     Instant createdAt,
                     Instant updatedAt,
                     UUID userId) {
      super(id, createdAt, updatedAt);
      this.userId = userId;
   }

   // 마지막 접속 시간이 현재 시간으로부터 5분 이내이면 현재 접속 중인 유저
   public boolean isOnline() {
      return getUpdatedAt()
            .plus(Duration.ofMinutes(5))
            .isAfter(Instant.now());
   }

}
