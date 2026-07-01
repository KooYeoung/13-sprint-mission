package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.MessageFile;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MessageDto(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String content,
        UUID channelId,
        UUID authorId,
        List<UUID> attachmentIds
) {
    public static MessageDto from(Message message) {
        return new MessageDto(
                message.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(message.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(message.getUpdatedAt()),
                message.getContent(),
                message.getChannel().getId(),
                message.getAuthor().getId(),
                message.getMessageFiles()
                        .stream()
                        .map(m -> m.getBinaryContent().getId())
                        .toList()
        );
    }

    public static MessageDto from(Message message, List<MessageFile> messageFiles) {
        return new MessageDto(
                message.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(message.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(message.getUpdatedAt()),
                message.getContent(),
                message.getChannel().getId(),
                message.getAuthor().getId(),
                messageFiles.stream()
                        .map(m -> m.getBinaryContent().getId())
                        .toList()
        );
    }
}