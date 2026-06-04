package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class BasicUserService implements UserService {
   private final UserRepository userRepository;
   private final BinaryContentRepository binaryContentRepository;
   private final UserStatusRepository userStatusRepository;

   @Override
   public UserResponse create(UserCreateRequest requestDto) {

      List<User> userList = userRepository.findAll();
      // username 과 email 존재 여부 확인 존재시 저장 x
      Predicate<User> isExistUsername = user -> user.getUsername().equals(requestDto.username()) ;
      Predicate<User> isExistEmail = user ->  user.getEmail().equals(requestDto.email());

      existThrow(isExistEmail, userList, "이미 존재하는 이메일 입니다.");
      existThrow(isExistUsername, userList, "이미 존재하는 아이디 입니다.");

      // 이미지 저장
      UUID imageId = null;
      if(requestDto.image() != null && !requestDto.image().isEmpty()){

         BinaryContent binaryContent = BinaryContent.builder()
               .originalFileName(requestDto.image().getOriginalFilename())
               .fileName(UUID.randomUUID().toString().replace("-", ""))
               .contentType(requestDto.image().getContentType())
               .build();

         binaryContentRepository.save(binaryContent);
         imageId = binaryContent.getId();
      }

      User user = requestDto.toUser(imageId);
      // USER 저장
      userRepository.save(user);

      // UserStatus 저장.
      Instant now = Instant.now();
      UserStatus userStatus = new UserStatus( now,user.getId());
      userStatusRepository.save(userStatus);

      return UserResponse.from(user).withOnline(userStatus.isOnline());
   }

   @Override
   public UserResponse findById(UUID userId) {

      User user = getUserRequireThrow(userId);

      UserResponse userResponse = UserResponse.from(user);
      userResponse = updateUserOnlineStatus(user, userResponse);

      return userResponse;
   }


   @Override
   public List<UserResponse> findAll() {

      return userRepository.findAll()
            .stream()
            .map( u -> {
               UserResponse userResponse = UserResponse.from(u);
               userResponse = updateUserOnlineStatus(u, userResponse);
               return userResponse;
            })
            .toList();
   }


   @Override
   public UserResponse update(UserUpdateRequest requestDto) {
      User user = getUserRequireThrow(requestDto.userId());

      // 이메일 검증.
      if(!user.hasEmail(requestDto.email())) {
         Predicate<User> isExistEmail = u -> u.getEmail().equals(requestDto.email());
         List<User> userList = userRepository.findAll();
         existThrow(isExistEmail,userList,"이미 존재하는 이메일 입니다.");
      }

      UUID imageId = user.getProfileImageId();
      if(requestDto.image() != null && !requestDto.image().isEmpty()){
         BinaryContent binaryContent = BinaryContent.builder()
               .originalFileName(requestDto.image().getOriginalFilename())
               .fileName(UUID.randomUUID().toString().replace("-", ""))
               .contentType(requestDto.image().getContentType())
               .build();

         binaryContentRepository.save(binaryContent);
         imageId = binaryContent.getId();
      }

      if(user.isProfileImageExist() && !imageId.equals(user.getProfileImageId())) {
         binaryContentRepository.delete(user.getProfileImageId());
      }

      User updatedUser = user.withNickname(requestDto.nickname())
            .withRealName(requestDto.realName())
            .withPassword(requestDto.password().isEmpty() ? user.getPassword() : requestDto.password())
            .withEmail(requestDto.email())
            .withPhoneNumber(requestDto.phoneNumber())
            .withProfileImageId(imageId)
            .withUpdatedAt(Instant.now());

      userRepository.save(updatedUser);

      return UserResponse.from(updatedUser);
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

   private UserResponse updateUserOnlineStatus(User u, UserResponse userResponse) {
      Optional<UserStatus> userStatus = userStatusRepository.findById(u.getId());
      if(userStatus.isPresent()) {
         userResponse = userResponse.withOnline(userStatus.get().isOnline());
      }
      return userResponse;
   }

}
