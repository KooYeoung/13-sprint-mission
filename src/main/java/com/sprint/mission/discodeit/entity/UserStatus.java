package com.sprint.mission.discodeit.entity;

import lombok.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.UUID;

@Getter
@ToString
public class UserStatus extends UpdatableEntity {

   private final UUID userId;
   public UserStatus(Instant now,
                      UUID userId) {
      super(now);
      this.userId = userId;
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

   public UserStatus withUpdatedAt(Instant now) {
      return new UserStatus(
            getId(),
            getCreatedAt(),
            now,
            userId
      );
   }

}
