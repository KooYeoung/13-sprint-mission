package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BinaryContentDto(
        UUID id,
        OffsetDateTime createdAt,
        Long size,
        String contentType,
        byte[] bytes
) {
    public static BinaryContentDto from(BinaryContent binaryContent, byte[] bytes) {
        return new BinaryContentDto(
                binaryContent.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(binaryContent.getCreatedAt()),
                binaryContent.getSize(),
                binaryContent.getContentType(),
                bytes
        );
    }
}