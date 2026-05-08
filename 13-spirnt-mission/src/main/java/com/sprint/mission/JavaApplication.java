package com.sprint.mission;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.file.FileChannelService;
import com.sprint.mission.discodeit.service.file.FileMessageService;
import com.sprint.mission.discodeit.service.file.FileUserService;
import com.sprint.mission.discodeit.service.jcf.JCFChannelService;
import com.sprint.mission.discodeit.service.jcf.JCFMessageService;
import com.sprint.mission.discodeit.service.jcf.JCFUserService;

import java.util.List;

public class JavaApplication {
   
   public static void main(String[] args) {
      UserService userService = new JCFUserService();
      ChannelService channelService = new JCFChannelService();
      MessageService messageService = new JCFMessageService(userService, channelService);

      runUserServiceCrudTest(userService);

      runChannelServiceCrudTest(channelService);

      runMessageServiceCrudTest(userService, channelService, messageService);

   }

   private static void runMessageServiceCrudTest(UserService userService, ChannelService channelService, MessageService messageService) {
      // 기존 등록한 유저 및 채널을 가져옴.
      // 유저와 채널이 없을경우 메시지 등록은 미진행.
      User user = userService.findAll().get(0);
      Channel channel = channelService.findAll().get(0);

      //1. 메시지 단일 등록
      Message message = createMessage(user, channel);
      messageService.save(message);
      System.out.println("message = " + message);

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         messageService.save(createMessage(user, channel, i));
      }

      //3. 단일 조회
      Message foundMessage = messageService.findById(message.getId());
      System.out.println("foundMessage = " + foundMessage);

      //4. 다건 조회
      List<Message> messageList = messageService.findAll();
      for (Message m : messageList){
         System.out.println("foundMessage = " + m);
      }

      //5. 수정
      messageService.update(foundMessage.getId()
            ,"updateMessageContent");

      //6. 수정 조회
      Message updateFoundMessage = messageService.findById(foundMessage.getId());
      System.out.println("updateFoundMessage = " + updateFoundMessage);

      //7. 삭제
      messageService.delete(updateFoundMessage.getId());

      //8. 삭제 후 재 조회
      Message deleteMessage = messageService.findById(updateFoundMessage.getId());
      System.out.println("deleteMessage = " + deleteMessage);
      System.out.println("deleteMessage = " + (deleteMessage == null));
   }


   private static void runChannelServiceCrudTest(ChannelService channelService) {
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


   private static Message createMessage(User user, Channel channel, int i) {
      String count = i == 0 ? "" : ""+i;
      return new Message("message" + count, user, channel);
   }

   private static Message createMessage(User user, Channel channel) {
      return createMessage(user, channel, 0);
   }


}