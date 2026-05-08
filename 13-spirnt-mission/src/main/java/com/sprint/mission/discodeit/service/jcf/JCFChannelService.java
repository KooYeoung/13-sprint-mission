package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.service.ChannelService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JCFChannelService implements ChannelService {

   private final static HashMap<UUID, Channel> channelHashMap = new HashMap<>();

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
   public void update(UUID channelId, String channelName, String description, ChannelType channelType) {
      Channel byId = findById(channelId);
      if(byId == null) return;

      byId.update(channelName,description,channelType);

      channelHashMap.put(byId.getId(), byId);

   }

   @Override
   public void delete(UUID channelId) {
      Channel byId = findById(channelId);
      if(byId == null) return;

      channelHashMap.remove(byId.getId());
   }
}
