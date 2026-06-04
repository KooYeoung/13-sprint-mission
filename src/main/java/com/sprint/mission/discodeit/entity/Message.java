package com.sprint.mission.discodeit.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.With;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@ToString
public class Message extends UpdatableEntity {
   @With
   private final String content;
   private final UUID userId;
   private final UUID channelId;
   @With
   private final List<UUID> fileIds;

   @Builder
   public Message(String content, UUID userId, UUID channelId, List<UUID> fileIds) {
      super(Instant.now());
      this.content = content;
      this.userId = userId;
      this.channelId = channelId;
      this.fileIds = fileIds;
   }

   private Message(UUID id
         , Instant createdAt
         , Instant updatedAt
         , String content
         , UUID userId
         , UUID channelId
         , List<UUID> fileIds) {
      super(id, createdAt, updatedAt);
      this.content = content;
      this.userId = userId;
      this.channelId = channelId;
      this.fileIds = fileIds;
   }

   public Message withUpdatedAt(Instant now){
      return new Message(
            getId(),
            getCreatedAt(),
            now,
            content,
            userId,
            channelId,
            fileIds
      );
   }

}
