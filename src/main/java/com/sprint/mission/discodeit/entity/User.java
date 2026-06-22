package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString(exclude = "password",callSuper = true)
public class User extends UpdatableEntity {

   private final String username;
   private final String nickname;
   private final String realName;
   private final String password;
   private final String email;
   private final String phoneNumber;
   private final UUID profileImageId;

   public User(UserCreateCommand command, UUID profileImageId) {
      super(Instant.now());
      this.username = command.username();
      this.nickname = command.nickname();
      this.realName = command.realName();
      this.password = command.password();
      this.email = command.email();
      this.phoneNumber = command.phoneNumber();
      this.profileImageId = profileImageId;
   }

   public User updateInfo(
          UserUpdateCommand command, UUID profileImageId
   ) {
      return new User(
              getId(),
              getCreatedAt(),
              Instant.now(),
              keepIfBlank(command.username(), username),
              keepIfBlank(command.nickname(), nickname),
              keepIfBlank(command.realName(), realName),
              keepIfBlank(command.password(), password),
              keepIfBlank(command.email(), email),
              keepIfBlank(command.phoneNumber(), phoneNumber),
              profileImageId
      );
   }
   private User(
         UUID id,
         Instant createdAt,
         Instant updatedAt,
         String username,
         String nickname,
         String realName,
         String password,
         String email,
         String phoneNumber,
         UUID profileImageId
   ) {
      super(id, createdAt, updatedAt);
      this.username = username;
      this.nickname = nickname;
      this.realName = realName;
      this.password = password;
      this.email = email;
      this.phoneNumber = phoneNumber;
      this.profileImageId = profileImageId;
   }

   public boolean isProfileImageExist(){
      return profileImageId != null;
   }

   public boolean hasEmail(String email){
      return this.email.equals(email);
   }

   public boolean hasUsername(String username) {
      return this.username.equals(username);
   }

   private String keepIfBlank(String newValue, String oldValue) {
      if (newValue == null || newValue.isBlank()) {
         return oldValue;
      }
      return newValue;
   }
}
