package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.OffsetDateTime;
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
                message.getChannelId(),
                message.getUserId(),
                message.getFileIds()
        );
    }
}