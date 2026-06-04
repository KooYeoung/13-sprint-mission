package com.sprint.mission.discodeit.entity;

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
public class UpdatableEntity extends BaseEntity implements Serializable {

   private final Instant updatedAt;

   public UpdatableEntity(UUID id, Instant createdAt, Instant updatedAt) {
      super(id, createdAt);
      this.updatedAt = updatedAt;
   }

   public UpdatableEntity(Instant now) {
      super(now);
      this.updatedAt = now;
   }

}
