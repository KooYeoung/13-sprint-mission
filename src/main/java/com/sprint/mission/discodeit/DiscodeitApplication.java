package com.sprint.mission.discodeit;

import com.sprint.mission.discodeit.config.RepositoryProperties;
import com.sprint.mission.discodeit.dto.request.*;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
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
      RepositoryProperties bean = ac.getBean(RepositoryProperties.class);


      log.info("========================= {} TEST START =========================", bean.getType().toUpperCase());
      runServiceIntegrationTest(userService, channelService, messageService);
      log.info("========================= {} TEST END =========================", bean.getType().toUpperCase());

   }


   private static void runServiceIntegrationTest(UserService userService, ChannelService channelService, MessageService messageService) {
      runUserServiceCrudTest(userService);
      runChannelServiceCrudTest(channelService);
      runMessageServiceCrudTest(userService, channelService, messageService);
   }

   private static void runMessageServiceCrudTest(UserService userService, ChannelService channelService, MessageService messageService) {
      // 기존 등록한 유저 및 채널을 가져옴.
      // 유저와 채널이 없을경우 메시지 등록은 미진행.
      UserDto user = userService.findAll().get(0);
      ChannelDto channel = channelService.findAll().get(0);
      log.info("user = {}", user);
      log.info("channel = {}", channel);

      //1. 메시지 단일 등록
      MessageCreateRequest messageCreateRequest = createMessage(user.id(), channel.id());
      MessageDto messageDto = messageService.save(MessageDto.from(messageCreateRequest), null);
      log.info("messageResponse = {}", messageDto);

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         messageService.save(MessageDto.from(createMessage(user.id(), channel.id(), i)),null);
      }

      //3. 단일 조회
      MessageDto foundMessageDto = messageService.findById(messageDto.messageId());
      log.info("foundMessageResponse = {}", foundMessageDto);

      //4. 다건 조회
      List<MessageDto> messageList = messageService.findAll();
      for (MessageDto m : messageList) {
         log.info("messageResponse = {}", m);
      }

      //5. 수정
      MessageUpdateRequest updateMessageContent = new MessageUpdateRequest(
            foundMessageDto.messageId()
            , "updateMessageContent"
            , foundMessageDto.channelId()
            , foundMessageDto.userId());
      MessageDto updatedMessageDto = messageService.update(MessageDto.from(updateMessageContent));
      log.info("updatedMessageResponse = {}", updatedMessageDto);

      //7. 삭제
      messageService.delete(updatedMessageDto.messageId());
      try {
         //8. 삭제 후 재 조회
         MessageDto deleteMessage = messageService.findById(updatedMessageDto.messageId());
         log.info("deleteMessage = {}", deleteMessage);
         log.info("deleteMessage = {}", (deleteMessage == null));
      } catch (IllegalArgumentException e) {
         log.error("IllegalArgumentException = {}", e.getMessage());
      }
   }


   private static void runChannelServiceCrudTest(ChannelService channelService) {
      //1. 채널 단일 등록
      ChannelCreateRequest channelRequest = channelCreate();
      ChannelDto savedChannel = channelService.save(ChannelDto.from(channelRequest));
      log.info("savedChannel = {}", savedChannel);

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         channelService.save(ChannelDto.from(channelCreate(i)));
      }

      //3. 단일 조회
      ChannelDto foundChannel = channelService.findById(savedChannel.id());
      log.info("foundChannel = {}", foundChannel);

      //4. 다건 조회
      List<ChannelDto> channelList = channelService.findAll();
      for (ChannelDto c : channelList) {
         log.info("foundChannel = {}", c);
      }

      ChannelUpdateRequest channelUpdateRequest = new ChannelUpdateRequest(
            foundChannel.id()
            , "updateChannelName"
            , foundChannel.description()
            , foundChannel.channelType().toString()
      );
      //5. 수정
      ChannelDto updatedChannelDto = channelService.update(ChannelDto.from(channelUpdateRequest));
      log.info("updatedChannelResponse = {}", updatedChannelDto);

      //6. 삭제
      channelService.delete(updatedChannelDto.id());

      try {
         //7. 삭제 후 재 조회
         ChannelDto deleteChannel = channelService.findById(updatedChannelDto.id());
         log.info("deleteChannel = {}", deleteChannel);
         log.info("deleteChannel = {}", (deleteChannel == null));
      } catch (IllegalArgumentException e) {
         log.error("IllegalArgumentException = {}", e.getMessage());
      }
   }


   private static void runUserServiceCrudTest(UserService userService) {
      //1. 유저 단일 등록
      UserCreateRequest requestDto = userCreate();
      UserDto user;
      try {
         user = userService.create(UserDto.from(requestDto), null);
      }catch (IllegalArgumentException e){
         log.error("IllegalArgumentException = {}", e.getMessage());
         return;
      }

      //2. 리스트 조회를 위해 다량 등록.
      for (int i = 1; i <= 10; i++) {
         UserCreateRequest userCreateRequest = userCreate(i);
         userService.create( UserDto.from(userCreateRequest),null);
      }

      //3. 단일 조회
      UserDto foundUser = userService.findById(user.id());
      log.info("foundUser = {}", foundUser);

      //4. 다건 조회
      List<UserDto> userList = userService.findAll();
      for (UserDto u : userList) {
         log.info("foundUser = {}", u);
      }

      UserUpdateRequest updateRequest = new UserUpdateRequest(foundUser.id(), "updateNickname", foundUser.realName(), "", foundUser.email(), foundUser.phoneNumber());

      //5. 수정
      UserDto updateFoundUser = userService.update(UserDto.from(updateRequest), null);
      log.info("updateFoundUser = {}", updateFoundUser);

      //6. 삭제
      userService.delete(updateFoundUser.id());

      //7. 삭제 후 재 조회
      try {
         UserDto deleteUser = userService.findById(updateFoundUser.id());
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
              , "nickname" + count
              , "realName" + count
              , "password"
              , count + "email@email"
              , "000-0000-0000"
      );
   }

   private static UserCreateRequest userCreate() {
      return userCreate(0);
   }

   private static ChannelCreateRequest channelCreate(int i) {
      String count = i == 0 ? "" : "" + i;
      return new ChannelCreateRequest("channelName" + count
            , "channelDescription" + count
            , i % 2 == 0 ? ChannelType.PUBLIC.name() : ChannelType.PRIVATE.name());
   }

   private static ChannelCreateRequest channelCreate() {
      return channelCreate(0);
   }


   private static MessageCreateRequest createMessage(UUID userId, UUID channelId, int i) {
      String count = i == 0 ? "" : "" + i;
      return new MessageCreateRequest("message" + count, channelId, userId);
   }

   private static MessageCreateRequest createMessage(UUID userId, UUID channelId) {

      return createMessage(userId, channelId, 0);
   }

}
