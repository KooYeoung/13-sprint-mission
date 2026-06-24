package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import lombok.With;

@With
public record UserUpdateRequest(
    String nickname
   ,  String realName
   ,  String password
   ,  String email
   ,  String phoneNumber
)
{
    public UserUpdateCommand toCommand(){
        return new UserUpdateCommand(nickname, realName, password, email, phoneNumber);
    }
}
