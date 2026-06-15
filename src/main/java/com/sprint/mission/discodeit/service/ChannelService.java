package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.response.ChannelDto;

import java.util.List;
import java.util.UUID;

public interface ChannelService {
   ChannelDto save(ChannelDto channelDto);
   ChannelDto findById(UUID channelId);
   List<ChannelDto> findAllByUserId(UUID userId);
   List<ChannelDto> findAll();
   ChannelDto update(ChannelDto channelDto);
   void delete(UUID channelId);
}
