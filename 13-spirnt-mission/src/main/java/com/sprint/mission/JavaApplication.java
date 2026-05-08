package com.sprint.mission;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

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



   }

   private static User userCreate(int i) {
      String count = i == 0 ? "" : ""+i;
      return new User("username" + count,"password","email@email","000-0000-0000","realName"+ count, "nickname"+ count);
   }
   private static User userCreate(){
      return  userCreate(0);
   }


}