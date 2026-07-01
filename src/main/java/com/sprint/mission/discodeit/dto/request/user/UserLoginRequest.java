package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserLoginCommand;

public record UserLoginRequest(
        String username,
        String password
) {

    public UserLoginCommand toCommand() {
        return new UserLoginCommand(username, password);
    }
}
