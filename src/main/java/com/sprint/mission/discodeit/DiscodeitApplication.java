package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.dto.request.*;
import com.sprint.mission.discodeit.dto.response.ChannelResponse;
import com.sprint.mission.discodeit.dto.response.MessageResponse;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@SpringBootApplication
public class DiscodeitApplication {

   @PostConstruct
   public void init() {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
   }

   public static void main(String[] args) {

      ApplicationContext ac = SpringApplication.run(DiscodeitApplication.class, args);

      UserService userService = ac.getBean(UserService.class);
      ChannelService channelService = ac.getBean(ChannelService.class);
      MessageService messageService = ac.getBean(MessageService.class);


      log.info("========================= FILE TEST START =========================");

      runServiceIntegrationTest(userService, channelService, messageService);
      log.info("========================= FILE TEST END =========================");

   }


   private static void runServiceIntegrationTest(UserService userService, ChannelService channelService, MessageService messageService) {
      runUserServiceCrudTest(userService);
      runChannelServiceCrudTest(channelService);
      runMessageServiceCrudTest(userService, channelService, messageService);
   }

   private static void runMessageServiceCrudTest(UserService userService, ChannelService channelService, MessageService messageService) {
      // 기존 등록한 유저 및 채널을 가져옴.
      // 유저와 채널이 없을경우 메시지 등록은 미진행.
      UserResponse user = userService.findAll().get(0);
      ChannelResponse channel = channelService.findAll().get(0);
      log.info("user = {}", user);
      log.info("channel = {}", channel);

      //1. 메시지 단일 등록
      MessageCreateRequest messageCreateRequest = createMessage(user.id(), channel.id());
      MessageResponse messageResponse = messageService.save(messageCreateRequest);
      log.info("messageResponse = {}", messageResponse);

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         messageService.save(createMessage(user.id(), channel.id(), i));
      }

      //3. 단일 조회
      MessageResponse foundMessageResponse = messageService.findById(messageResponse.messageId());
      log.info("foundMessageResponse = {}", foundMessageResponse);

      //4. 다건 조회
      List<MessageResponse> messageList = messageService.findAll();
      for (MessageResponse m : messageList) {
         log.info("messageResponse = {}", m);
      }

      //5. 수정
      MessageUpdateRequest updateMessageContent = new MessageUpdateRequest(
            foundMessageResponse.messageId()
            , "updateMessageContent"
            , foundMessageResponse.channelId()
            , foundMessageResponse.userId());
      MessageResponse updatedMessageResponse = messageService.update(updateMessageContent);
      log.info("updatedMessageResponse = {}", updatedMessageResponse);

      //7. 삭제
      messageService.delete(updatedMessageResponse.messageId());
      try {
         //8. 삭제 후 재 조회
         MessageResponse deleteMessage = messageService.findById(updatedMessageResponse.messageId());
         log.info("deleteMessage = {}", deleteMessage);
         log.info("deleteMessage = {}", (deleteMessage == null));
      } catch (IllegalArgumentException e) {
         log.error("IllegalArgumentException = {}", e.getMessage());
      }
   }


   private static void runChannelServiceCrudTest(ChannelService channelService) {
      //1. 채널 단일 등록
      ChannelCreateRequest channelRequest = channelCreate();
      ChannelResponse savedChannel = channelService.save(channelRequest);
      log.info("savedChannel = {}", savedChannel);

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         channelService.save(channelCreate(i));
      }

      //3. 단일 조회
      ChannelResponse foundChannel = channelService.findById(savedChannel.id());
      log.info("foundChannel = {}", foundChannel);

      //4. 다건 조회
      List<ChannelResponse> channelList = channelService.findAll();
      for (ChannelResponse c : channelList) {
         log.info("foundChannel = {}", c);
      }

      ChannelUpdateRequest channelUpdateRequest = new ChannelUpdateRequest(
            foundChannel.id()
            , "updateChannelName"
            , foundChannel.description()
            , foundChannel.channelType()
      );
      //5. 수정
      ChannelResponse updatedChannelResponse = channelService.update(channelUpdateRequest);
      log.info("updatedChannelResponse = {}", updatedChannelResponse);

      //6. 삭제
      channelService.delete(updatedChannelResponse.id());

      try {
         //7. 삭제 후 재 조회
         ChannelResponse deleteChannel = channelService.findById(updatedChannelResponse.id());
         log.info("deleteChannel = {}", deleteChannel);
         log.info("deleteChannel = {}", (deleteChannel == null));
      } catch (IllegalArgumentException e) {
         log.error("IllegalArgumentException = {}", e.getMessage());
      }
   }


   private static void runUserServiceCrudTest(UserService userService) {
      //1. 유저 단일 등록
      UserCreateRequest requestDto = userCreate();
      UserResponse user;
      try {
         user = userService.create(requestDto);
      }catch (IllegalArgumentException e){
         log.error("IllegalArgumentException = {}", e.getMessage());
         return;
      }

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         userService.create(userCreate(i));
      }

      //3. 단일 조회
      UserResponse foundUser = userService.findById(user.id());
      log.info("foundUser = {}", foundUser);

      //4. 다건 조회
      List<UserResponse> userList = userService.findAll();
      for (UserResponse u : userList) {
         log.info("foundUser = {}", u);
      }

      UserUpdateRequest updateRequest = new UserUpdateRequest(foundUser.id(), "updateNickname", foundUser.realName(), "", foundUser.email(), foundUser.phoneNumber(), null);

      //5. 수정
      UserResponse updateFoundUser = userService.update(updateRequest);
      log.info("updateFoundUser = {}", updateFoundUser);

      //6. 삭제
      userService.delete(updateFoundUser.id());

      //7. 삭제 후 재 조회
      try {
         UserResponse deleteUser = userService.findById(updateFoundUser.id());
         log.info("deleteUser = {}", deleteUser);
         log.info("deleteUser = {}", (deleteUser == null));
      } catch (IllegalArgumentException e) {
         log.error("IllegalArgumentException = {}", e.getMessage());
      }

   }

   private static UserCreateRequest userCreate(int i) {
      String count = i == 0 ? "" : "" + i;
      return new UserCreateRequest(
            "username" + count
            , "password"
            , count + "email@email"
            , "000-0000-0000"
            , "realName" + count
            , "nickname" + count
            , null
      );
   }

   private static UserCreateRequest userCreate() {
      return userCreate(0);
   }

   private static ChannelCreateRequest channelCreate(int i) {
      String count = i == 0 ? "" : "" + i;
      return new ChannelCreateRequest("channelName" + count
            , "channelDescription" + count
            , i % 2 == 0 ? ChannelType.PUBLIC : ChannelType.PRIVATE);
   }

   private static ChannelCreateRequest channelCreate() {
      return channelCreate(0);
   }


   private static MessageCreateRequest createMessage(UUID userId, UUID channelId, int i) {
      String count = i == 0 ? "" : "" + i;
      return new MessageCreateRequest("message" + count, channelId, userId, new ArrayList<>());
   }

   private static MessageCreateRequest createMessage(UUID userId, UUID channelId) {

      return createMessage(userId, channelId, 0);
   }

}
