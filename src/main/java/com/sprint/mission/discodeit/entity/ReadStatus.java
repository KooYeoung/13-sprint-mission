package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.ReadStatusUpdateCommand;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public class ReadStatus extends UpdatableEntity {
   private final UUID userId;
   private final UUID channelId;

   public ReadStatus(ReadStatusCreateCommand command) {
      super(command.readAt());
      this.userId = command.userId();
      this.channelId = command.channelId();
   }

   private ReadStatus(UUID id, Instant createdAt, Instant updatedAt, UUID userId, UUID channelId) {
      super(id, createdAt, updatedAt);
      this.userId = userId;
      this.channelId = channelId;
   }

   public ReadStatus updateInfo(ReadStatusUpdateCommand command){
      return  new ReadStatus(
              getId()
              ,getCreatedAt()
              ,command.readAt()
              ,userId
              ,channelId
      );
   }

}
