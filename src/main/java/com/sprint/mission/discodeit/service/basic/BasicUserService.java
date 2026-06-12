package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
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
   private final BinaryContentRepository binaryContentRepository;
   private final UserStatusRepository userStatusRepository;

   @Override
   public UserDto create(UserDto userDto, MultipartFile file) {

      List<User> userList = userRepository.findAll();
      // username 과 email 존재 여부 확인 존재시 저장 x
      Predicate<User> isExistUsername = user -> user.getUsername().equals(userDto.username()) ;
      Predicate<User> isExistEmail = user ->  user.getEmail().equals(userDto.email());

      existThrow(isExistEmail, userList, "이미 존재하는 이메일 입니다.");
      existThrow(isExistUsername, userList, "이미 존재하는 아이디 입니다.");

      // 이미지 저장
      UUID imageId = null;
      if(file != null && !file.isEmpty()){

         BinaryContent binaryContent = BinaryContent.builder()
               .originalFileName(file.getOriginalFilename())
               .contentType(file.getContentType())
               .build();

         binaryContentRepository.save(binaryContent);
         imageId = binaryContent.getId();
      }
      UserDto updatedUserDto = userDto.withProfileImageId(imageId);

      UserCreateCommand newUserCommand = UserCreateCommand.from(updatedUserDto);

      User user = new User(newUserCommand);
      // USER 저장
      userRepository.save(user);

      // UserStatus 저장.
      Instant now = Instant.now();
      UserStatus userStatus = new UserStatus(new UserStatusCreateCommand(user.getId(), now));
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
   public UserDto update(UserDto userDto, MultipartFile file) {
      User user = getUserRequireThrow(userDto.id());

      // 이메일 검증.
      if(!user.hasEmail(userDto.email())) {
         Predicate<User> isExistEmail = u -> u.getEmail().equals(userDto.email());
         List<User> userList = userRepository.findAll();
         existThrow(isExistEmail,userList,"이미 존재하는 이메일 입니다.");
      }

      UUID imageId = user.getProfileImageId();
      if(file != null && !file.isEmpty()){
         BinaryContent binaryContent = BinaryContent.builder()
               .originalFileName(file.getOriginalFilename())
               .contentType(file.getContentType())
               .build();

         binaryContentRepository.save(binaryContent);
         imageId = binaryContent.getId();
      }

      if(user.isProfileImageExist() && !imageId.equals(user.getProfileImageId())) {
         binaryContentRepository.delete(user.getProfileImageId());
      }
      UserDto updatedUserDto = userDto.withProfileImageId(imageId);

      User updatedUser = user.updateInfo(UserUpdateCommand.from(updatedUserDto));

      userRepository.save(updatedUser);

      return UserDto.from(updatedUser);
   }

   private  boolean isBlank(String password) {
      return password == null || password.isEmpty();
   }

   @Override
   public void delete(UUID userId) {
      User user = getUserRequireThrow(userId);

      userRepository.delete(user.getId());
      if(user.isProfileImageExist()) {
         binaryContentRepository.delete(user.getProfileImageId());
      }
   }

   private void existThrow(Predicate<User> p, List<User> userList, String message) {
      boolean exist = userList.stream()
            .anyMatch(p);
      if(exist) throw new IllegalArgumentException(message);
   }

   private User getUserRequireThrow(UUID userId) {
      return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 입니다."));
   }

   private UserDto updateUserOnlineStatus(User u, UserDto userDto) {
      Optional<UserStatus> userStatus = userStatusRepository.findByUserId(u.getId());
      if(userStatus.isPresent()) {
         userDto = userDto.withOnline(userStatus.get().isOnline());
      }
      return userDto;
   }

}
