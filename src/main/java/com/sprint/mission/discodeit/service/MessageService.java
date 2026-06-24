package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MessageService {
   MessageDto save(MessageCreateCommand command, List<MultipartFile> files);
   MessageDto findById(UUID messageId);
   List<MessageDto>  findAll();
   List<MessageDto>  findAllByChannelId(UUID channelId);
   MessageDto update(UUID messageId, MessageUpdateCommand command);
   void delete(UUID messageId);
}
