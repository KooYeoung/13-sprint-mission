package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JCFMessageService implements MessageService {

   private final static HashMap<UUID, Message> messageHashMap = new HashMap<>();
   private final UserService userService;
   private final ChannelService channelService;

   public JCFMessageService(UserService userService, ChannelService channelService) {
      this.userService = userService;
      this.channelService = channelService;
   }

   @Override
   public void save(Message message) {
      if(isUserOrChannelMissing(message.getUser().getId(), message.getChannel().getId())) return;

      messageHashMap.put(message.getId(), message);
   }


   @Override
   public Message findById(UUID messageId) {

      return messageHashMap.get(messageId);
   }

   @Override
   public List<Message> findAll() {
      return messageHashMap.values().stream().toList();
   }

   @Override
   public void update(UUID messageId, String content) {
      Message byId = findById(messageId);
      if(byId == null) return;

      if(isUserOrChannelMissing(byId.getUser().getId(), byId.getChannel().getId())) return;

      byId.update(content);
   }

   @Override
   public void delete(UUID messageId) {
      Message byId = findById(messageId);
      if(byId == null) return;

      if(isUserOrChannelMissing(byId.getUser().getId(), byId.getChannel().getId())) return;

      messageHashMap.remove(byId.getId());
   }

   private boolean isUserOrChannelMissing(UUID userId, UUID channelId) {
      User user = userService.findById(userId);
      Channel channel = channelService.findById(channelId);
      return user == null || channel == null;
   }
}
