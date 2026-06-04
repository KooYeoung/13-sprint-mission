package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public class ReadStatus extends UpdatableEntity {
   private final UUID userId;
   private final UUID channelId;

   public ReadStatus(Instant now, UUID userId, UUID channelId) {
      super(now);
      this.userId = userId;
      this.channelId = channelId;
   }

   private ReadStatus(UUID id, Instant createdAt, Instant updatedAt, UUID userId, UUID channelId) {
      super(id, createdAt, updatedAt);
      this.userId = userId;
      this.channelId = channelId;
   }

   public ReadStatus withUpdatedAt(Instant now){
      return new ReadStatus(
            getId(),
            getCreatedAt(),
            now,
            userId,
            channelId
      );
   }

}
