package com.sprint.mission.discodeit.config;

import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.file.FileChannelRepository;
import com.sprint.mission.discodeit.repository.file.FileMessageRepository;
import com.sprint.mission.discodeit.repository.file.FileUserRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFChannelRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFMessageRepository;
import com.sprint.mission.discodeit.repository.jcf.JCFUserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.basic.BasicChannelService;
import com.sprint.mission.discodeit.service.basic.BasicMessageService;
import com.sprint.mission.discodeit.service.basic.BasicUserService;

import java.util.HashMap;

import static com.sprint.mission.discodeit.config.RepositoryType.*;

public class AppConfig {
   private final HashMap<RepositoryType, UserRepository> userRepositoryHashMap = new HashMap<>();
   private final HashMap<RepositoryType, ChannelRepository> channelRepositoryHashMap = new HashMap<>();
   private final HashMap<RepositoryType, MessageRepository>  messageRepositoryHashMap= new HashMap<>();

   public AppConfig() {
      userRepositoryHashMap.put(FILE, new FileUserRepository());
      userRepositoryHashMap.put(JCF,  new JCFUserRepository());

      channelRepositoryHashMap.put(FILE, new FileChannelRepository());
      channelRepositoryHashMap.put(JCF, new JCFChannelRepository());

      messageRepositoryHashMap.put(FILE, new FileMessageRepository());
      messageRepositoryHashMap.put(FILE, new JCFMessageRepository());
   }

   private UserRepository userRepository(RepositoryType repositoryType) {
      return userRepositoryHashMap.get(repositoryType);
   }

   public UserService userService(RepositoryType repositoryType) {
      return new BasicUserService(userRepository(repositoryType));
   }

   private ChannelRepository channelRepository(RepositoryType repositoryType) {
      return channelRepositoryHashMap.get(repositoryType);
   }

   public ChannelService channelService(RepositoryType repositoryType) {
      return new BasicChannelService(channelRepository(repositoryType));
   }

   private MessageRepository messageRepository(RepositoryType repositoryType) {
      return messageRepositoryHashMap.get(repositoryType);
   }

   public MessageService messageService(RepositoryType repositoryType) {
      return new BasicMessageService(messageRepository(repositoryType), userRepository(repositoryType), channelRepository(repositoryType));
   }






}
