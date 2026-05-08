package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;

import java.util.List;
import java.util.UUID;

public interface ChannelService {
   void save(Channel channel);
   Channel findById(UUID channelId);
   List<Channel> findAll();
   void update(UUID channelId, String channelName, String description, ChannelType channelType);
   void delete(UUID channelId);
}
