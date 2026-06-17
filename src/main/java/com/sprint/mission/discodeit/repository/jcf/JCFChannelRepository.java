package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.repository.ChannelRepository;
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
public class JCFChannelRepository implements ChannelRepository {
   private final Map<UUID, Channel> channelMap = new HashMap<>();

   @Override
   public void save(Channel channel) {
      channelMap.put(channel.getId(), channel);
   }

   @Override
   public Optional<Channel> findById(UUID channelId) {
      return Optional.ofNullable(channelMap.get(channelId));
   }

   @Override
   public List<Channel> findAll() {
      return channelMap.values().stream().toList();
   }

   @Override
   public void delete(UUID channelId) {
      channelMap.remove(channelId);
   }
}
