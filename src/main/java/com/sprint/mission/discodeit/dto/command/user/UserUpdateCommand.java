package com.sprint.mission.discodeit.dto.command.user;

import com.sprint.mission.discodeit.dto.response.UserDto;

import java.time.Instant;
import java.util.UUID;

public record UserUpdateCommand(
        String nickname
        , String realName
        , String password
        , String email
        , String phoneNumber
        , UUID profileImageId
         , Instant updateAt
        ) {
    public static UserUpdateCommand from (UserDto userDto){
        return new UserUpdateCommand(
                 userDto.nickname()
                , userDto.realName()
                , userDto.password()
                , userDto.email()
                , userDto.phoneNumber()
                , userDto.profileId()
                ,Instant.now()
        );
    }
}
