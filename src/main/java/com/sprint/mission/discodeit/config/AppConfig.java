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
import java.util.Map;

import static com.sprint.mission.discodeit.config.RepositoryType.*;

public class AppConfig {
   private final Map<RepositoryType, UserRepository> userRepositoryMap = new HashMap<>();
   private final Map<RepositoryType, ChannelRepository> channelRepositoryMap = new HashMap<>();
   private final Map<RepositoryType, MessageRepository> messageRepositoryMap = new HashMap<>();

   public AppConfig() {
      userRepositoryMap.put(FILE, new FileUserRepository());
      userRepositoryMap.put(JCF,  new JCFUserRepository());

      channelRepositoryMap.put(FILE, new FileChannelRepository());
      channelRepositoryMap.put(JCF, new JCFChannelRepository());

      messageRepositoryMap.put(FILE, new FileMessageRepository());
      messageRepositoryMap.put(JCF, new JCFMessageRepository());
   }

   private UserRepository userRepository(RepositoryType repositoryType) {
      return userRepositoryMap.get(repositoryType);
   }

   public UserService userService(RepositoryType repositoryType) {
      return new BasicUserService(userRepository(repositoryType));
   }

   private ChannelRepository channelRepository(RepositoryType repositoryType) {
      return channelRepositoryMap.get(repositoryType);
   }

   public ChannelService channelService(RepositoryType repositoryType) {
      return new BasicChannelService(channelRepository(repositoryType));
   }

   private MessageRepository messageRepository(RepositoryType repositoryType) {
      return messageRepositoryMap.get(repositoryType);
   }

   public MessageService messageService(RepositoryType repositoryType) {
      return new BasicMessageService(messageRepository(repositoryType), userRepository(repositoryType), channelRepository(repositoryType));
   }






}
