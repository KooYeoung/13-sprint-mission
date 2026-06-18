package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {
   UserDto create(UserCreateCommand command, MultipartFile file);
   UserDto findById(UUID userId);
   List<UserDto> findAll();
   UserDto update(UUID userId, UserUpdateCommand command, MultipartFile file);
   void delete(UUID userId);
}
