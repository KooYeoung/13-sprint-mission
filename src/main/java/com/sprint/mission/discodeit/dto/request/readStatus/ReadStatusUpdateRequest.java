package com.sprint.mission.discodeit.dto.request.readStatus;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusUpdateRequest (
        Instant readAt,
        UUID userId
      ){

      public ReadStatusUpdateCommand toCommand(){
            return new ReadStatusUpdateCommand(readAt);
      }

}
