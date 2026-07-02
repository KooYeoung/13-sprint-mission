package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.MessageFile;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageMapper {
    private final UserMapper userMapper;
    private final BinaryContentMapper binaryContentMapper;

    public MessageDto toDto(Message message) {
        return toDto(
                message,
                message.getMessageFiles()
        );
    }

    public MessageDto toDto(Message message, List<MessageFile> messageFiles) {
        return new MessageDto(
                message.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(message.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(message.getUpdatedAt()),
                message.getContent(),
                message.getChannelId(),
                userMapper.toDto(message.getAuthor()),
                convertToBinaryContentList(messageFiles)
        );
    }

    private @NonNull List<BinaryContentDto> convertToBinaryContentList(List<MessageFile> messageFiles) {
        return messageFiles.stream()
                .map(m -> binaryContentMapper.toDto(m.getBinaryContent()))
                .toList();
    }
}
