package com.sprint.mission.discodeit.dto.response;

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

}