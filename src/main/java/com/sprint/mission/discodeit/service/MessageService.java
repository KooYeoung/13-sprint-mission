package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageService {
    MessageDto save(MessageCreateCommand command, List<MultipartFile> files);

    MessageDto findById(UUID messageId);

    PageResponse<MessageDto> findAllByChannelId(UUID channelId, Pageable pageable);

    MessageDto update(UUID messageId, MessageUpdateCommand command);

    void delete(UUID messageId);

    void deleteAllByChannelId(UUID channelId);

}
