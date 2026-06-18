package com.sprint.mission.discodeit.dto.command.user;

public record UserCreateCommand(
        String username
        , String nickname
        , String realName
        , String password
        , String email
        , String phoneNumber
        ) {
}
