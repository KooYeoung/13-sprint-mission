package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
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

   private final String content;
   private final UUID userId;
   private final UUID channelId;
   private final List<UUID> fileIds;

   @Builder
   public Message(MessageCreateCommand command) {
      super(Instant.now());
      this.content = command.content();
      this.userId = command.userId();
      this.channelId = command.channelId();
      this.fileIds = command.fileIds();
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

   public Message updateInfo(MessageUpdateCommand command){
      return new Message(
            getId(),
            getCreatedAt(),
            Instant.now(),
            content,
            userId,
            channelId,
            fileIds
      );
   }

}
