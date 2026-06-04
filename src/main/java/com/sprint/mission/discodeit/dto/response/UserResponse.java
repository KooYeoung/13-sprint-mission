package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.User;
import lombok.With;

import java.util.UUID;

public record UserResponse(
      UUID id
      , String username
      , String nickname
      , String realName
      , String email
      , String phoneNumber
      , UUID profileImageId
      , @With boolean isOnline
) {
   public static UserResponse from(User user) {
      return new UserResponse(user.getId()
            , user.getUsername()
            , user.getNickname()
            , user.getRealName()
            , user.getEmail()
            , user.getPhoneNumber()
            , user.getProfileImageId()
            , false);
   }

}
