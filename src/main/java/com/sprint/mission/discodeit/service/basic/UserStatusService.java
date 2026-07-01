package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.exception.userStatus.UserStatusBadRequestException;
import com.sprint.mission.discodeit.exception.userStatus.UserStatusNotFoundException;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserStatusService {
   private final UserStatusRepository userStatusRepository;
   private final UserRepository userRepository;

   public UserStatusDto create(UUID userId, UserStatusCreateCommand command) {

      getUserRequireThrow(userId);
      boolean userStatusExists = userStatusRepository.findAll().stream()
            .anyMatch(u -> u.getUserId().equals(userId));

      if (userStatusExists) throw new UserStatusBadRequestException("이미 유저 상태가 존재합니다.");

      UserStatus userStatus = new UserStatus(userId, command);
      userStatusRepository.save(userStatus);

      return UserStatusDto.from(userStatus);
   }

   public UserStatusDto findById(UUID userStatusId, UUID userId) {
      UserStatus userStatus = getUserStatusRequireThrow(userStatusId,userId);

      return UserStatusDto.from(userStatus);
   }

   public List<UserStatusDto> findAll() {

      return userStatusRepository.findAll()
            .stream()
            .map(UserStatusDto::from)
            .toList();

   }

   public UserStatusDto update(UUID userStatusId ,UUID userId, UserStatusUpdateCommand command){
      Optional<UserStatus> statusOptional = userStatusRepository.findByIdAndUserId(userStatusId, userId);
      UserStatus status;
      if (statusOptional.isPresent()) {
         UserStatus currentStatus = statusOptional.get();
         UserStatus updatedUserStatus = currentStatus.updateInfo(command);
         status = userStatusRepository.update(updatedUserStatus);
      } else {
         return create(userId, new UserStatusCreateCommand(command.updateAt()));
      }

      return UserStatusDto.from(status);
   }

   public UserStatusDto updateByUserId(UUID userId, UserStatusUpdateCommand command) {

      getUserRequireThrow(userId);
      Optional<UserStatus> userStatusResult = userStatusRepository.findByUserId(userId);

      UserStatus status;
      if (userStatusResult.isPresent()) {
         UserStatus userStatus = userStatusResult.get();
         UserStatus updatedUserStatus = userStatus.updateInfo(command);
         status = userStatusRepository.update(updatedUserStatus);
      } else {
         UserStatus userStatus = new UserStatus(userId, new UserStatusCreateCommand(command.updateAt()));
         status = userStatusRepository.save(userStatus);
      }
      return UserStatusDto.from(status);
   }

   public void delete(UUID userStatusId, UUID userId) {
      getUserStatusRequireThrow(userStatusId, userId);
      userStatusRepository.delete(userStatusId);

   }

   private UserStatus getUserStatusRequireThrow(UUID userStatusId, UUID userId) {
      return userStatusRepository.findByIdAndUserId(userStatusId, userId).orElseThrow(UserStatusNotFoundException::new);
   }

   private User getUserRequireThrow(UUID userId) {
      return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
   }

}
