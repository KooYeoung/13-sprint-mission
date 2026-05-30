package com.sprint.mission.discodeit.entity;

import java.io.Serializable;
import java.util.UUID;

public class BaseEntity implements Serializable {
   private final UUID id; // 식별자
   private final Long createdAt;
   private Long updatedAt;

   public BaseEntity(){
      id = UUID.randomUUID();
      createdAt = System.currentTimeMillis();
   }
   public void update(){
      updatedAt = System.currentTimeMillis();
   }

   public UUID getId() {
      return id;
   }

   public Long getCreatedAt() {
      return createdAt;
   }

   public Long getUpdatedAt() {
      return updatedAt;
   }
}
