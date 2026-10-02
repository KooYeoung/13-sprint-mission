package com.sprint.mission.discodeit.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        OffsetDateTime createdAt,
        UUID receiverId,
        String title,
        String content
) {
}
