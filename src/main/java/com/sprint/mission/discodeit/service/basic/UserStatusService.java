package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
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

   public UserStatusDto create(UserStatusDto userStatusDto) {

      getUserRequireThrow(userStatusDto.userId());
      boolean userStatusExists = userStatusRepository.findAll().stream()
            .anyMatch(u -> u.getUserId().equals(userStatusDto.userId()));

      if (userStatusExists) throw new IllegalArgumentException("이미 유저 상태가 존재합니다.");


      UserStatus userStatus = new UserStatus(UserStatusCreateCommand.from(userStatusDto));
      userStatusRepository.save(userStatus);

      return UserStatusDto.from(userStatus);
   }

   public UserStatusDto findById(UUID id) {
      UserStatus userStatus = getUserStatusRequireThrow(id);

      return UserStatusDto.from(userStatus);
   }

   public List<UserStatusDto> findAll() {

      return userStatusRepository.findAll()
            .stream()
            .map(UserStatusDto::from)
            .toList();

   }

   public UserStatusDto update(UserStatusDto dto){

      Optional<UserStatus> statusOptional = userStatusRepository.findById(dto.id());
      UserStatus status;
      if (statusOptional.isPresent()) {
         UserStatus currentStatus = statusOptional.get();
         UserStatus updatedUserStatus = currentStatus.updateInfo(new UserStatusUpdateCommand(dto.lastOnlineAt()));
         status = userStatusRepository.update(updatedUserStatus);
      } else {
         getUserRequireThrow(dto.userId());
         UserStatus userStatus = new UserStatus( new UserStatusCreateCommand(dto.userId(), dto.lastOnlineAt()));
         status = userStatusRepository.save(userStatus);
      }

      return UserStatusDto.from(status);
   }

   public UserStatusDto updateByUserId(UUID userId) {

      getUserRequireThrow(userId);
      Optional<UserStatus> userStatusResult = userStatusRepository.findByUserId(userId);

      Instant now = Instant.now();

      UserStatus status;
      if (userStatusResult.isPresent()) {
         UserStatus userStatus = userStatusResult.get();
         UserStatus updatedUserStatus = userStatus.updateInfo(new UserStatusUpdateCommand(now));
         status = userStatusRepository.update(updatedUserStatus);
      } else {
         UserStatus userStatus = new UserStatus(new UserStatusCreateCommand(userId, now));
         status = userStatusRepository.save(userStatus);
      }
      return UserStatusDto.from(status);
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
