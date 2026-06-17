package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MessageService {
   MessageDto save(MessageDto messageDto, List<MultipartFile> files);
   MessageDto findById(UUID messageId);
   List<MessageDto>  findAll();
   List<MessageDto>  findAllByChannelId(UUID channelId);
   MessageDto update(MessageDto messageDto);
   void delete(UUID messageId);
}
