package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.MessageFile;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import org.jspecify.annotations.NonNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MessageDto(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String content,
        UUID channelId,
        UserDto author,
        List<BinaryContentDto> attachments
) {
    public static MessageDto from(Message message) {
        return new MessageDto(
                message.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(message.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(message.getUpdatedAt()),
                message.getContent(),
                message.getChannelId(),
                UserDto.from(message.getAuthor()),
                convertToBinaryContentList(message.getMessageFiles())
        );
    }

    public static MessageDto from(Message message, List<MessageFile> messageFiles) {
        return new MessageDto(
                message.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(message.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(message.getUpdatedAt()),
                message.getContent(),
                message.getChannelId(),
                UserDto.from(message.getAuthor()),
                convertToBinaryContentList(messageFiles)
        );
    }

    private static @NonNull List<BinaryContentDto> convertToBinaryContentList(List<MessageFile> messageFiles) {
        return messageFiles.stream()
                .map(m -> BinaryContentDto.from(m.getBinaryContent()))
                .toList();
    }
}