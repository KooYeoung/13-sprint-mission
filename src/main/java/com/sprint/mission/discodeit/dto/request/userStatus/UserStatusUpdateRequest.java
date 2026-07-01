package com.sprint.mission.discodeit.dto.request.userStatus;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;

import java.time.Instant;

public record UserStatusUpdateRequest(
        Instant newLastActiveAt
) {

    public UserStatusUpdateCommand toCommand() {
        return new UserStatusUpdateCommand(newLastActiveAt);
    }
}