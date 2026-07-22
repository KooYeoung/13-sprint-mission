package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserLoginCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequest(
        @NotBlank(message = ValidationMessage.USER_NAME)
        String username,
        @NotBlank(message = ValidationMessage.USER_PASSWORD)
        String password
) {

    public UserLoginCommand toCommand() {
        return new UserLoginCommand(username, password);
    }
}
