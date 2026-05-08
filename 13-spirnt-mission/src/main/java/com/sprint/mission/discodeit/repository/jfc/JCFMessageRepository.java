package com.sprint.mission.discodeit.repository.jfc;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.MessageRepository;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JCFMessageRepository implements MessageRepository {
   private final HashMap<UUID, Message> messageHashMap = new HashMap<>();

   @Override
   public void save(Message message) {
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
   public void delete(UUID messageId) {
      messageHashMap.remove(messageId);
   }
}
