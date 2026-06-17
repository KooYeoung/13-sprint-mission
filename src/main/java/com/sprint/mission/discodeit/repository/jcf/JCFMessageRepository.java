package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.MessageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@ConditionalOnProperty(
      prefix = "discodeit.repository",
      name = "type",
      havingValue = "jcf",
      matchIfMissing = true
)
public class JCFMessageRepository implements MessageRepository {
   private final Map<UUID, Message> messageMap = new HashMap<>();

   @Override
   public void save(Message message) {
      messageMap.put(message.getId(), message);
   }

   @Override
   public Optional<Message> findById(UUID messageId) {
      return Optional.ofNullable(messageMap.get(messageId));
   }

   @Override
   public List<Message> findAll() {
      return messageMap.values().stream().toList();
   }

   @Override
   public void delete(UUID messageId) {
      messageMap.remove(messageId);
   }

   @Override
   public void deleteAllByChannelId(UUID channelId) {
      findAllByChannelId(channelId)
            .stream()
            .map(Message::getId)
            .forEach(this::delete);
   }

   @Override
   public List<Message> findAllByChannelId(UUID channelId) {
      return findAll().stream()
            .filter(m -> m.getChannelId().equals(channelId)).toList();
   }
}
