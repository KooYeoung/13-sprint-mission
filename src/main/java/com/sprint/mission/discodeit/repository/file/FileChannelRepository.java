package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.repository.ChannelRepository;
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
public class FileChannelRepository implements ChannelRepository {

   private final FileObjectStorage<Channel> storage =
         new FileObjectStorage<>("data/channels");

   @Override
   public void save(Channel channel) {
      storage.save(channel);
   }

   @Override
   public Optional<Channel> findById(UUID channelId) {
      return Optional.ofNullable(storage.load(channelId)) ;
   }

   @Override
   public List<Channel> findAll() {
      return storage.loadAll();
   }

   @Override
   public void delete(UUID channelId) {
      storage.delete(channelId);
   }
}
