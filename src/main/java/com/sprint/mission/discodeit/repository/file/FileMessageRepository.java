package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.MessageRepository;

import java.util.List;
import java.util.UUID;

public class FileMessageRepository implements MessageRepository {
   private final FileObjectStorage<Message> storage = new FileObjectStorage<>("data/messages");

   @Override
   public void save(Message message) {
      storage.save(message);
   }

   @Override
   public Message findById(UUID messageId) {
      return storage.load(messageId);
   }

   @Override
   public List<Message> findAll() {
      return storage.loadAll();
   }

   @Override
   public void delete(UUID messageId) {
      storage.delete(messageId);
   }
}
