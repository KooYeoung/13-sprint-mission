package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.MessageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(
      prefix = "discodeit.repository",
      name = "type",
      havingValue = "file"
)
public class FileMessageRepository implements MessageRepository {
   private final FileObjectStorage<Message> storage = new FileObjectStorage<>("data/messages");

   @Override
   public void save(Message message) {
      storage.save(message);
   }

   @Override
   public Optional<Message> findById(UUID messageId) {
      return Optional.ofNullable(storage.load(messageId));
   }

   @Override
   public List<Message> findAll() {
      return storage.loadAll();
   }

   @Override
   public void delete(UUID messageId) {
      storage.delete(messageId);
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
      return findAll()
            .stream()
            .filter(m -> m.getChannelId().equals(channelId)).toList();
   }
}
