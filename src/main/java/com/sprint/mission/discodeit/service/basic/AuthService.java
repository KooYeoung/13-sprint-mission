package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.user.UserLoginCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.user.LoginFailException;
import com.sprint.mission.discodeit.exception.user.UserError;
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

   public UserDto login(UserLoginCommand command) {
      User user = userRepository.findAll()
            .stream()
            .filter(u -> u.getUsername().equals(command.username()))
            .filter(u -> u.getPassword().equals(command.password()))
            .findFirst()
            .orElseThrow(() -> new LoginFailException(UserError.LOGIN.getMessage()));

      Instant now = Instant.now();

      Optional<UserStatus> optionalUserStatus = userStatusRepository.findByUserId(user.getId());
      UserStatus userStatus;

      if (optionalUserStatus.isPresent()) {
         UserStatus status = optionalUserStatus.get().updateInfo(new UserStatusUpdateCommand(now));
         userStatus = userStatusRepository.update(status);
      } else {
         UserStatus status = new UserStatus( user.getId(), new UserStatusCreateCommand( now));
         userStatus = userStatusRepository.save(status);
      }

      return UserDto.from(user).withOnline(userStatus.isOnline());
   }
}
