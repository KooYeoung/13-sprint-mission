package com.sprint.mission.discodeit.entity;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString(exclude = "password")
public class User extends UpdatableEntity {

   private final String username;
   @With
   private final String nickname;
   @With
   private final String realName;
   @With
   private final String password;
   @With
   private final String email;
   @With
   private final String phoneNumber;
   @With
   private final UUID profileImageId;

   @Builder
   public User(
         String username,
         String nickname,
         String realName,
         String password,
         String email,
         String phoneNumber,
         UUID profileImageId
   ) {
      super(Instant.now());
      this.username = username;
      this.nickname = nickname;
      this.realName = realName;
      this.password = password;
      this.email = email;
      this.phoneNumber = phoneNumber;
      this.profileImageId = profileImageId;
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

   public User withUpdatedAt(Instant now){
      return new User(
            getId(),
            getCreatedAt(),
            now,
            username,
            nickname,
            realName,
            password,
            email,
            phoneNumber,
            profileImageId
      );
   }

}
