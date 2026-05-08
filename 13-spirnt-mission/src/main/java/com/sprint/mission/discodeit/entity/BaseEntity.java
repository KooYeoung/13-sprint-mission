package com.sprint.mission.discodeit.entity;

import java.util.UUID;

public class BaseEntity {
   private UUID id; // 식별자
   private Long createdAt;
   private Long updatedAt;

   BaseEntity(){
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
