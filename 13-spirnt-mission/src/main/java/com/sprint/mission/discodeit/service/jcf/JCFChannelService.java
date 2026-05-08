package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.jfc.JCFChannelRepository;
import com.sprint.mission.discodeit.service.ChannelService;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JCFChannelService implements ChannelService {
   private final ChannelRepository channelRepository = new JCFChannelRepository();


   @Override
   public void save(Channel channel) {
      channelRepository.save(channel);

   }

   @Override
   public Channel findById(UUID channelId) {
      return channelRepository.findById(channelId);
   }

   @Override
   public List<Channel> findAll() {
      return channelRepository.findAll();
   }

   @Override
   public void update(UUID channelId, String channelName, String description, ChannelType channelType) {
      Channel byId = findById(channelId);
      if(byId == null) return;

      byId.update(channelName,description,channelType);

      channelRepository.save(byId);
   }

   @Override
   public void delete(UUID channelId) {
      Channel byId = findById(channelId);
      if(byId == null) return;

      channelRepository.delete(byId.getId());

   }
}
