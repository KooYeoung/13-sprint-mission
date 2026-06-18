package com.sprint.mission.discodeit.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sprint.mission.discodeit.entity.User;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id
      , String username
      , String nickname
      , String realName
      , String email
      , String phoneNumber
      , @JsonIgnore String password
      , @With UUID profileId
      , @With boolean online
      , Instant createdAt
        ,Instant updatedAt
        ) {

   public static UserDto from(User user) {
      return new UserDto(
              user.getId()
            , user.getUsername()
            , user.getNickname()
            , user.getRealName()
            , user.getEmail()
            , user.getPhoneNumber()
              ,user.getPassword()
            , user.getProfileImageId()
            , false
            ,user.getCreatedAt()
              ,user.getUpdatedAt()
      );
   }

}
