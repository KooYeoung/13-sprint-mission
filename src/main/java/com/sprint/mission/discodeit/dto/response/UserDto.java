package com.sprint.mission.discodeit.dto.response;

import lombok.With;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        BinaryContentDto profile,
        boolean online,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {


}
