package com.sprint.mission.discodeit.dto.request.readStatus;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;

import java.time.Instant;

public record ReadStatusUpdateRequest(
        Instant newLastReadAt
) {
      public ReadStatusUpdateCommand toCommand() {
            return new ReadStatusUpdateCommand(newLastReadAt);
      }
}