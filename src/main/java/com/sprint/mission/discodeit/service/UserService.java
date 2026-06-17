package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {
   UserDto create(UserDto userDto,MultipartFile file);
   UserDto findById(UUID userId);
   List<UserDto> findAll();
   UserDto update(UserDto userDto, MultipartFile file);
   void delete(UUID userId);
}
