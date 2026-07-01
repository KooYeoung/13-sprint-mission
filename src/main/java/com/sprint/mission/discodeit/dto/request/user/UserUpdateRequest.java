package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import lombok.With;

@With
public record UserUpdateRequest(
        String newUsername,
        String newPassword,
        String newEmail
) {
    public UserUpdateCommand toCommand() {
        return new UserUpdateCommand(newUsername, newPassword, newEmail);
    }
}
