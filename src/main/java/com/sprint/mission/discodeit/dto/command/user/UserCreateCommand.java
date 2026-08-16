package com.sprint.mission.discodeit.dto.command.user;

public record UserCreateCommand(
        String username,
        String password,
        String email
) {
}
