package com.sprint.mission.discodeit.dto.command.user;

public record UserUpdateCommand(
        String username,
        String password,
        String email
) {
}
