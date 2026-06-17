package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.request.UserLoginRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
   private final UserRepository userRepository;
   private final UserStatusRepository userStatusRepository;

   public UserDto login(UserDto userDto) {
      User user = userRepository.findAll()
            .stream()
            .filter(u -> u.getUsername().equals(userDto.username()))
            .filter(u -> u.getPassword().equals(userDto.password()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("아이디 와 비밀번호를 다시한번 확인해 주세요."));

      Instant now = Instant.now();

      Optional<UserStatus> optionalUserStatus = userStatusRepository.findByUserId(user.getId());
      UserStatus userStatus;

      if (optionalUserStatus.isPresent()) {
         UserStatus status = optionalUserStatus.get().updateInfo(new UserStatusUpdateCommand(now));
         userStatus = userStatusRepository.update(status);
      } else {
         UserStatus status = new UserStatus(new UserStatusCreateCommand(user.getId(), now));
         userStatus = userStatusRepository.save(status);
      }

      return UserDto.from(user).withOnline(userStatus.isOnline());
   }
}
