package com.sprint.mission;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

import java.util.List;

public class JavaApplication {
   
   public static void main(String[] args) {
      UserService userService = new JCFUserService();

      //1. 유저 단일 등록
      User user = userCreate();
      userService.save(user);

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         userService.save(userCreate(i));
      }

      //3. 단일 조회
      User foundUser = userService.findById(user.getId());
      System.out.println("foundUser = " + foundUser);

      //4. 다건 조회
      List<User> userList = userService.findAll();
      for (User u : userList){
         System.out.println("foundUser = " + u);
      }

      //5. 수정
      userService.update(foundUser.getId()
            ,"updateNickname"
            , foundUser.getRealName()
            , foundUser.getPassword()
            , foundUser.getEmail()
            , foundUser.getPhoneNumber());

      //6. 수정 조회
      User updateFoundUser = userService.findById(foundUser.getId());
      System.out.println("updateFoundUser = " + updateFoundUser);

      //7. 삭제
      userService.delete(updateFoundUser.getId());

      //8. 삭제 후 재 조회
      User deleteUser = userService.findById(updateFoundUser.getId());
      System.out.println("deleteUser = " + deleteUser);
      System.out.println("deleteUser = " + (deleteUser == null));


   }

   private static User userCreate(int i) {
      String count = i == 0 ? "" : ""+i;
      return new User("username" + count,"password","email@email","000-0000-0000","realName"+ count, "nickname"+ count);
   }
   private static User userCreate(){
      return  userCreate(0);
   }


}