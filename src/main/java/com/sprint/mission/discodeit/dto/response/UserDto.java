package com.sprint.mission.discodeit.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserLoginRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
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
      , @With boolean isOnline
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

   public static UserDto from(UserCreateRequest request){
      return new UserDto(
              null
              ,request.username()
              ,request.nickname()
              ,request.realName()
              ,request.email()
              ,request.phoneNumber()
              ,request.password()
              ,null
              ,false
              ,null
              ,null
      );
   }

   public static UserDto from(UserUpdateRequest request){
      return new UserDto(
              request.userId()
              ,null
              ,request.nickname()
              ,request.realName()
              ,request.email()
              ,request.phoneNumber()
              ,request.password()
              ,null
              ,false
              ,null
              ,null
      );
   }

   public static UserDto from(UserLoginRequest request){
      return new UserDto(
              null
              ,request.username()
              ,null
              ,null
              ,null
              ,null
              ,request.password()
              ,null
              ,false
              ,null
              ,null
      );
   }

}
