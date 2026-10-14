package com.sprint.mission.discodeit.dto.request.readStatus;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReadStatusUpdateRequest(
        @NotNull(message = ValidationMessage.TIME_MESSAGE)
        Instant newLastReadAt,
        @NotNull(message = ValidationMessage.NOT_NULL_MESSAGE)
        Boolean newNotificationEnabled
) {
    public ReadStatusUpdateCommand toCommand() {
        return new ReadStatusUpdateCommand(newLastReadAt, newNotificationEnabled);
    }
}