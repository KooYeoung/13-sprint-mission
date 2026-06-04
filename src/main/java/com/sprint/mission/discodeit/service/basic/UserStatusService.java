package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserStatusResponse;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserStatusService {
   private final UserStatusRepository userStatusRepository;
   private final UserRepository userRepository;

   public UserStatusResponse create(UserStatusCreateRequest request) {

      getUserRequireThrow(request.userId());
      boolean userStatusExists = userStatusRepository.findAll().stream()
            .anyMatch(u -> u.getUserId().equals(request.userId()));

      if (userStatusExists) throw new IllegalArgumentException("이미 유저 상태가 존재합니다.");

      UserStatus userStatus = request.toUserStatus();
      userStatusRepository.save(userStatus);

      return UserStatusResponse.from(userStatus);
   }

   public UserStatusResponse findById(UUID id) {
      UserStatus userStatus = getUserStatusRequireThrow(id);

      return UserStatusResponse.from(userStatus);
   }

   public List<UserStatusResponse> findAll() {

      return userStatusRepository.findAll()
            .stream()
            .map(UserStatusResponse::from)
            .toList();

   }

   public void update(UserStatusUpdateRequest request) {

      Optional<UserStatus> statusOptional = userStatusRepository.findById(request.id());
      if (statusOptional.isPresent()) {
         UserStatus currentStatus = statusOptional.get();
         UserStatus updatedUserStatus = currentStatus.withUpdatedAt(request.lastOnlineAt());
         userStatusRepository.update(updatedUserStatus);
      } else {
         getUserRequireThrow(request.userId());
         UserStatus userStatus = new UserStatus( request.lastOnlineAt() , request.userId());
         userStatusRepository.save(userStatus);
      }

   }

   public void updateByUserId(UUID userId) {

      getUserRequireThrow(userId);
      Optional<UserStatus> userStatusResult = userStatusRepository.findByUserId(userId);

      Instant now = Instant.now();

      if (userStatusResult.isPresent()) {
         UserStatus userStatus = userStatusResult.get();
         UserStatus updatedUserStatus = userStatus.withUpdatedAt(now);
         userStatusRepository.update(updatedUserStatus);
      } else {
         UserStatus userStatus = new UserStatus(now, userId);
         userStatusRepository.save(userStatus);
      }

   }

   public void delete(UUID id) {
      getUserStatusRequireThrow(id);
      userStatusRepository.delete(id);

   }

   private UserStatus getUserStatusRequireThrow(UUID id) {
      return userStatusRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("유저 상태가 존재하지 않습니다."));
   }

   private User getUserRequireThrow(UUID userId) {
      return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("존재 하지 않는 유저 입니다."));
   }

}
