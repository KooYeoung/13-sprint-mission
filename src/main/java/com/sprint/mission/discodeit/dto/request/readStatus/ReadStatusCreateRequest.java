package com.sprint.mission.discodeit.dto.request.readStatus;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusCreateRequest(
      UUID userId
      , Instant readAt
) {
    public ReadStatusCreateCommand toCommand(){
        return new ReadStatusCreateCommand(userId, readAt);
    }


}
