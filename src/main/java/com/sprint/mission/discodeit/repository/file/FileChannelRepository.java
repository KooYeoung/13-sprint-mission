package com.sprint.mission.discodeit.repository.file;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.repository.ChannelRepository;

import java.util.List;
import java.util.UUID;

public class FileChannelRepository implements ChannelRepository {

   private final FileObjectStorage<Channel> storage =
         new FileObjectStorage<>("data/channels");

   @Override
   public void save(Channel channel) {
      storage.save(channel);
   }

   @Override
   public Channel findById(UUID channelId) {
      return storage.load(channelId);
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
