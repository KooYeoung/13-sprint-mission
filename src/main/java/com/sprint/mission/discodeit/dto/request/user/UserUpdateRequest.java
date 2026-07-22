package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.With;

@With
public record UserUpdateRequest(
        @NotBlank(message = ValidationMessage.USER_NAME)
        String newUsername,
        String newPassword,
        @Email(message = ValidationMessage.USER_EMAIL)
        String newEmail
) {
    public UserUpdateCommand toCommand() {
        return new UserUpdateCommand(newUsername, newPassword, newEmail);
    }
}
