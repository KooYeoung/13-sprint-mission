package com.sprint.mission.discodeit.dto.command.user;

public record UserUpdateCommand(
        String username
        ,String nickname
        , String realName
        , String password
        , String email
        , String phoneNumber
        ) {
}
