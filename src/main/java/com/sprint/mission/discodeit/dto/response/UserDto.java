package com.sprint.mission.discodeit.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import lombok.With;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDto(
        UUID id
      , String username
      , String nickname
      , String realName
      , String email
      , String phoneNumber
      , @With UUID profileId
      , @With boolean online
      , OffsetDateTime createdAt
        ,OffsetDateTime updatedAt
        ) {

   public static UserDto from(User user) {
      return new UserDto(
              user.getId()
            , user.getUsername()
            , user.getNickname()
            , user.getRealName()
            , user.getEmail()
            , user.getPhoneNumber()
            , user.getProfileImageId()
            , false
            ,RequestTimeZoneUtils.toOffsetDateTime(user.getCreatedAt())
              ,RequestTimeZoneUtils.toOffsetDateTime(user.getUpdatedAt())
      );
   }



}
