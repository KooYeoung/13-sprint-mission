package com.sprint.mission.discodeit.entity;

public class User extends BaseEntity{
   private String username; // 로그인 id
   private String nickname; // 별명
   private String realName; // 실명
   private String password; // 비밀번호
   private String email; // 이메일
   private String phoneNumber; // 핸드폰 번호

   public User(String username, String password, String email, String phoneNumber, String realName,String nickname){
      this.username = username;
      this.password = password;
      this.email = email;
      this.phoneNumber = phoneNumber;
      this.realName = realName;
      this.nickname = nickname;
   }

   public void update(User updateUser){
      super.update();
      this.nickname = updateUser.nickname;
      this.realName = updateUser.realName;
      this.password = updateUser.password;
      this.email = updateUser.email;
      this.phoneNumber = updateUser.phoneNumber;
   }

   public String getUsername() {
      return username;
   }

   public String getNickname() {
      return nickname;
   }

   public String getRealName() {
      return realName;
   }

   public String getPassword() {
      return password;
   }

   public String getEmail() {
      return email;
   }

   public String getPhoneNumber() {
      return phoneNumber;
   }
}
