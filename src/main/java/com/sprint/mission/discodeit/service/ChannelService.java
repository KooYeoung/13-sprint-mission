package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.ChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.ChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ChannelResponse;

import java.util.List;
import java.util.UUID;

public interface ChannelService {
   ChannelResponse save(ChannelCreateRequest channelCreateRequest);
   ChannelResponse findById(UUID channelId);
   List<ChannelResponse> findAllByUserId(UUID userId);
   List<ChannelResponse> findAll();
   ChannelResponse update(ChannelUpdateRequest channelUpdateRequest);
   void delete(UUID channelId);
}
