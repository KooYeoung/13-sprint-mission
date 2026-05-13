package com.sprint.mission.discodeit.service.file;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.service.ChannelService;

import java.util.List;
import java.util.UUID;

public class FileChannelService implements ChannelService {

   private final ChannelRepository channelRepository;

   public FileChannelService(ChannelRepository channelRepository) {
      this.channelRepository = channelRepository;
   }

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
      Channel channel = findById(channelId);
      if (notExistChannel(channel)) return;

      channel.update(channelName, description, channelType);

      channelRepository.save(channel);
   }

   @Override
   public void delete(UUID channelId) {
      Channel channel = findById(channelId);
      if (notExistChannel(channel)) return;

      channelRepository.delete(channel.getId());
   }

   private boolean notExistChannel(Channel channel) {
      return channel == null;
   }
}
