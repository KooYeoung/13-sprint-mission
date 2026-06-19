package com.sprint.mission.discodeit.dto.request.readStatus;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusCreateRequest(
        UUID userId,
        UUID channelId,
        Instant lastReadAt
) {
    public ReadStatusCreateCommand toCommand() {
        return new ReadStatusCreateCommand(userId, lastReadAt);
    }
}