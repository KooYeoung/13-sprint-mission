package com.sprint.mission.discodeit.dto.request.user;

import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import lombok.With;

@With
public record UserUpdateRequest(
        String newUsername
        ,String nickname
   ,  String realName
   ,  String newPassword
   ,  String newEmail
   ,  String phoneNumber
)
{
    public UserUpdateCommand toCommand(){
        return new UserUpdateCommand(newUsername,nickname, realName, newPassword, newEmail, phoneNumber);
    }
}
