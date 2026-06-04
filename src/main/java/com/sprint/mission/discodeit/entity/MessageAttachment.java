package com.sprint.mission.discodeit.entity;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class MessageAttachment extends BaseEntity {
   private final UUID messageId;
   private final UUID fileId;

   public MessageAttachment(UUID messageId, UUID fileId) {
      super(Instant.now());
      this.messageId = messageId;
      this.fileId = fileId;
   }

}
