package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.UserLoginRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
   private final UserRepository userRepository;
   private final UserStatusRepository userStatusRepository;

   public UserResponse login(UserLoginRequest requestDto) {
      User user = userRepository.findAll()
            .stream()
            .filter(u -> u.getUsername().equals(requestDto.username()))
            .filter(u -> u.getPassword().equals(requestDto.password()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("아이디 와 비밀번호를 다시한번 확인해 주세요."));

      Instant now = Instant.now();

      Optional<UserStatus> optionalUserStatus = userStatusRepository.findByUserId(user.getId());
      UserStatus userStatus;

      if (optionalUserStatus.isPresent()) {
         userStatus = optionalUserStatus.get().withUpdatedAt(now);
         userStatusRepository.update(userStatus);
      } else {
         userStatus = new UserStatus( now, user.getId());
         userStatusRepository.save(userStatus);
      }

      return UserResponse.from(user).withOnline(userStatus.isOnline());
   }
}
