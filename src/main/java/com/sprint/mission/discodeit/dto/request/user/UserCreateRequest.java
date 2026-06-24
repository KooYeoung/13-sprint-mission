package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;

public record UserCreateRequest(
      String username
      , String nickname
      , String realName
      , String password
      , String email
      , String phoneNumber) {

    public UserCreateCommand toCommand(){
        return new UserCreateCommand(username, nickname,realName,password,email,phoneNumber);
    }

}
