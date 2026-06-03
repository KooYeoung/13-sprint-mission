package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;


import java.util.List;
import java.util.UUID;

public class JCFMessageService implements MessageService {

   private final MessageRepository messageRepository;
   private final UserRepository userRepository;
   private final ChannelRepository channelRepository;

   public JCFMessageService(MessageRepository messageRepository, UserRepository userRepository, ChannelRepository channelRepository) {
      this.messageRepository = messageRepository;
      this.userRepository = userRepository;
      this.channelRepository = channelRepository;
   }

   @Override
   public void save(Message message) {
      if (isUserOrChannelMissing(message.getUserId(), message.getChannelId())) return;
      messageRepository.save(message);
   }


   @Override
   public Message findById(UUID messageId) {
      Message message = messageRepository.findById(messageId);
      if (notExistMessage(message)) return null;
      User user = getUser(message.getUserId());
      Channel channel = getChannel(message.getChannelId());
      if (user == null || channel == null) return null;

      message.attach(user,channel);

      return message;
   }

   @Override
   public List<Message> findAll() {

      List<Message> all = messageRepository.findAll();
      all.forEach(message -> message.attach(getUser(message.getUserId()),getChannel(message.getChannelId())));

      return all;
   }

   @Override
   public void update(UUID messageId, String content) {
      Message message = findById(messageId);
      if (notExistMessage(message)) return;
      if (isUserOrChannelMissing(message.getUserId(), message.getChannelId())) return;

      message.update(content);
      messageRepository.save(message);
   }

   @Override
   public void delete(UUID messageId) {
      Message message = findById(messageId);
      if (notExistMessage(message)) return;
      if (isUserOrChannelMissing(message.getUserId(), message.getChannelId())) return;

      messageRepository.delete(message.getId());
   }

   private boolean isUserOrChannelMissing(UUID userId, UUID channelId) {
      User user = getUser(userId);
      Channel channel = getChannel(channelId);
      return user == null || channel == null;
   }

   private Channel getChannel(UUID channelId) {
      return channelRepository.findById(channelId);
   }

   private User getUser(UUID userId) {
      return userRepository.findById(userId);
   }

   private boolean notExistMessage(Message message) {
      return message == null;
   }

}
