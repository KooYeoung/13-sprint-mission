package com.sprint.mission.discodeit.dto.request.userStatus;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UserStatusUpdateRequest(
        @NotNull(message = ValidationMessage.TIME_MESSAGE)
        Instant newLastActiveAt
) {

    public UserStatusUpdateCommand toCommand() {
        return new UserStatusUpdateCommand(newLastActiveAt);
    }
}