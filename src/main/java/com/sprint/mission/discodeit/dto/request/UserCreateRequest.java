package com.sprint.mission.discodeit.dto.request;

public record UserCreateRequest(
      String username
      , String nickname
      , String realName
      , String password
      , String email
      , String phoneNumber) {

}
