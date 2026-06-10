package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public class BaseEntity implements Serializable {
   private final UUID id; // 식별자
   private final Instant createdAt;

   public BaseEntity(UUID id, Instant createdAt) {
      this.id = id;
      this.createdAt = createdAt;
   }
   public BaseEntity(Instant now) {
      this(UUID.randomUUID(), now);
   }



}
