package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record UserCreateRequest(
      String username
      , String nickname
      , String realName
      , String password
      , String email
      , String phoneNumber) {

}
