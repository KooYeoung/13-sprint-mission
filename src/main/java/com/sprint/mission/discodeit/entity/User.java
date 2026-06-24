package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import lombok.*;

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
              username,
              command.nickname(),
              command.realName(),
              command.password(),
              command.email(),
              command.phoneNumber(),
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

}
