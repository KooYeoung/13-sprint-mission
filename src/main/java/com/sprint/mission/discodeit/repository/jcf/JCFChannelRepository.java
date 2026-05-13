package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.repository.ChannelRepository;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JCFChannelRepository implements ChannelRepository {
   private final HashMap<UUID, Channel> channelHashMap = new HashMap<>();

   @Override
   public void save(Channel channel) {
      channelHashMap.put(channel.getId(), channel);
   }

   @Override
   public Channel findById(UUID channelId) {
      return channelHashMap.get(channelId);
   }

   @Override
   public List<Channel> findAll() {
      return channelHashMap.values().stream().toList();
   }

   @Override
   public void delete(UUID channelId) {
      channelHashMap.remove(channelId);
   }
}
