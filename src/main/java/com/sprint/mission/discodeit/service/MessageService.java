package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageResponse;
import com.sprint.mission.discodeit.entity.Message;

import java.util.List;
import java.util.UUID;

public interface MessageService {
   MessageResponse save(MessageCreateRequest messageCreateRequest);
   MessageResponse findById(UUID messageId);
   List<MessageResponse>  findAll();
   List<MessageResponse>  findAllByChannelId(UUID channelId);
   MessageResponse update(MessageUpdateRequest messageUpdateRequest);
   void delete(UUID messageId);
}
