package com.sprint.mission.discodeit.dto.command.user;

import com.sprint.mission.discodeit.dto.response.UserDto;

import java.util.UUID;

public record UserCreateCommand(
        String username
        , String nickname
        , String realName
        , String password
        , String email
        , String phoneNumber
        , UUID profileImageId
        ) {
    public static UserCreateCommand from (UserDto userDto){
        return new UserCreateCommand(
                userDto.username()
                , userDto.nickname()
                , userDto.realName()
                , userDto.password()
                , userDto.email()
                , userDto.phoneNumber()
                , userDto.profileImageId()
        );
    }
}
