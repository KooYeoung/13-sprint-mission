package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;

import java.util.List;
import java.util.UUID;

public class BasicMessageService implements MessageService {
   private final MessageRepository messageRepository;
   private final UserRepository userRepository;
   private final ChannelRepository channelRepository;

   public BasicMessageService(MessageRepository messageRepository, UserRepository userRepository, ChannelRepository channelRepository) {
      this.messageRepository = messageRepository;
      this.userRepository = userRepository;
      this.channelRepository = channelRepository;
   }

   @Override
   public void save(Message message) {
      if(isUserOrChannelMissing(message)) return;
      messageRepository.save(message);
   }

   @Override
   public Message findById(UUID messageId) {
      return messageRepository.findById(messageId);
   }

   @Override
   public List<Message> findAll() {
      return messageRepository.findAll();
   }

   @Override
   public void update(UUID messageId, String content) {
      Message message = findById(messageId);
      if(notExistMessage(message)) return;
      if(isUserOrChannelMissing(message)) return;
      message.update(content);
      messageRepository.save(message);
   }

   @Override
   public void delete(UUID messageId) {
      Message message = findById(messageId);
      if(notExistMessage(message)) return;
      if(isUserOrChannelMissing(message)) return;
      messageRepository.delete(messageId);
   }

   private boolean isUserOrChannelMissing(Message message) {
      User user = userRepository.findById(message.getUserId());
      Channel channel = channelRepository.findById(message.getChannelId());
      return user == null || channel == null;
   }

   private  boolean notExistMessage(Message message) {
      return message == null;
   }

}
