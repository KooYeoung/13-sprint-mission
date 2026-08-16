package com.sprint.mission.discodeit.dto.request.readStatus;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusCreateRequest(
        @NotNull(message = ValidationMessage.USER_ID_MESSAGE)
        UUID userId,
        @NotNull(message = ValidationMessage.CHANNEL_ID_MESSAGE)
        UUID channelId,
        @NotNull(message = ValidationMessage.TIME_MESSAGE)
        Instant lastReadAt
) {
    public ReadStatusCreateCommand toCommand() {
        return new ReadStatusCreateCommand(userId, lastReadAt);
    }

}

