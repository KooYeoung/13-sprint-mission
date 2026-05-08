package com.sprint.mission;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.jcf.JCFChannelService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

import java.util.List;

public class JavaApplication {
   
   public static void main(String[] args) {
      UserService userService = new JCFUserService();
      ChannelService channelService = new JCFChannelService();

      runUserServiceCrudTest(userService);

      //1. 채널 단일 등록
      Channel channel = channelCreate();
      channelService.save(channel);
      System.out.println("channel = " + channel);

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         channelService.save(channelCreate(i));
      }

      //3. 단일 조회
      Channel foundChannel = channelService.findById(channel.getId());
      System.out.println("foundChannel = " + foundChannel);

      //4. 다건 조회
      List<Channel> channelList = channelService.findAll();
      for (Channel c : channelList){
         System.out.println("foundChannel = " + c);
      }

      //5. 수정
      channelService.update(foundChannel.getId()
            ,"updateChannelName"
            , foundChannel.getDescription()
            , foundChannel.getChannelType());

      //6. 수정 조회
      Channel updateFoundChannel = channelService.findById(foundChannel.getId());
      System.out.println("updateFoundChannel = " + updateFoundChannel);

      //7. 삭제
      channelService.delete(updateFoundChannel.getId());

      //8. 삭제 후 재 조회
      Channel deleteChannel = channelService.findById(updateFoundChannel.getId());
      System.out.println("deleteChannel = " + deleteChannel);
      System.out.println("deleteChannel = " + (deleteChannel == null));





   }



   private static void runUserServiceCrudTest(UserService userService) {
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

   private static Channel channelCreate(int i) {
      String count = i == 0 ? "" : ""+i;
      return new Channel("channelName" + count, "channelDescription" + count, i % 2 == 0 ?  ChannelType.PUBLIC : ChannelType.PRIVATE);
   }

   private static Channel channelCreate(){
      return  channelCreate(0);
   }


}