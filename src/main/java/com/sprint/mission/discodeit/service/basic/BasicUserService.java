package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.UserBadRequestException;
import com.sprint.mission.discodeit.exception.UserNotFoundException;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasicUserService implements UserService {
   private final UserRepository userRepository;
   private final UserStatusRepository userStatusRepository;
   private final BinaryContentService binaryContentService;

   @Override
   public UserDto create(UserCreateCommand command, MultipartFile file) {

      List<User> userList = userRepository.findAll();
      // username 과 email 존재 여부 확인 존재시 저장 x
      Predicate<User> isExistUsername = user -> user.getUsername().equals(command.username()) ;
      Predicate<User> isExistEmail = user ->  user.getEmail().equals(command.email());

      existThrow(isExistEmail, userList, "이미 존재하는 이메일 입니다.");
      existThrow(isExistUsername, userList, "이미 존재하는 아이디 입니다.");

      UUID profileImageId = null;
      Optional<BinaryContentDto> binaryContentDto = binaryContentService.create(file);
      if(binaryContentDto.isPresent()){
         profileImageId = binaryContentDto.get().id();
      }

      User user = new User(command, profileImageId);
      // USER 저장
      userRepository.save(user);

      // UserStatus 저장.
      Instant now = Instant.now();
      UserStatus userStatus = new UserStatus(user.getId(), new UserStatusCreateCommand(now));
      userStatusRepository.save(userStatus);

      return UserDto.from(user).withOnline(userStatus.isOnline());
   }

   @Override
   public UserDto findById(UUID userId) {

      User user = getUserRequireThrow(userId);

      UserDto userDto = UserDto.from(user);
      userDto = updateUserOnlineStatus(user, userDto);

      return userDto;
   }

   @Override
   public List<UserDto> findAll() {

      return userRepository.findAll()
            .stream()
            .map( u -> {
               UserDto userDto = UserDto.from(u);
               userDto = updateUserOnlineStatus(u, userDto);
               return userDto;
            })
            .toList();
   }

   @Override
   public UserDto update(UUID userId,UserUpdateCommand command, MultipartFile file) {
      User user = getUserRequireThrow(userId);

      // 이메일 검증.
      if(!user.hasEmail(command.email())) {
         Predicate<User> isExistEmail = u -> u.getEmail().equals(command.email());
         List<User> userList = userRepository.findAll();
         existThrow(isExistEmail,userList,"이미 존재하는 이메일 입니다.");
      }

      UUID oldImageId = user.getProfileImageId();
      UUID newImageId = oldImageId;

      Optional<BinaryContentDto> binaryContentDto = binaryContentService.create(file);

      if (binaryContentDto.isPresent()) {
         newImageId = binaryContentDto.get().id();
      }

      User updatedUser = user.updateInfo(command, newImageId);

      userRepository.save(updatedUser);

      if (oldImageId != null && !oldImageId.equals(newImageId)) {
         binaryContentService.delete(oldImageId);
      }

      return UserDto.from(updatedUser);
   }

   @Override
   public void delete(UUID userId) {
      User user = getUserRequireThrow(userId);

      userRepository.delete(user.getId());
      if(user.isProfileImageExist()) {
         binaryContentService.delete(user.getProfileImageId());
      }
   }

   private void existThrow(Predicate<User> p, List<User> userList, String message) {
      boolean exist = userList.stream()
            .anyMatch(p);
      if(exist) throw new UserBadRequestException(message);
   }

   private User getUserRequireThrow(UUID userId) {
      return userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
   }

   private UserDto updateUserOnlineStatus(User u, UserDto userDto) {
      Optional<UserStatus> userStatus = userStatusRepository.findByUserId(u.getId());
      if(userStatus.isPresent()) {
         userDto = userDto.withOnline(userStatus.get().isOnline());
      }
      return userDto;
   }

}
