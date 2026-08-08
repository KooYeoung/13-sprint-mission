package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(
        @NotBlank(message = ValidationMessage.USER_NAME)
        String username,
        @NotBlank(message = ValidationMessage.USER_PASSWORD)
        String password,
        @Email(message = ValidationMessage.USER_EMAIL)
        @NotBlank(message = ValidationMessage.USER_EMAIL_REQUIRED_MESSAGE)
        String email
) {

    public UserCreateCommand toCommand() {
        return new UserCreateCommand(username, password, email);
    }

}
