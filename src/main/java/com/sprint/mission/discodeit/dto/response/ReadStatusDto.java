package com.sprint.mission.discodeit.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReadStatusDto(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID userId,
        UUID channelId,
        OffsetDateTime lastReadAt
) {

}