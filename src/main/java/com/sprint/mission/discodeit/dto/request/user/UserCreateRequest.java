package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;

public record UserCreateRequest(
        String username,
        String password,
        String email
) {

    public UserCreateCommand toCommand() {
        return new UserCreateCommand(username, password, email);
    }

}
