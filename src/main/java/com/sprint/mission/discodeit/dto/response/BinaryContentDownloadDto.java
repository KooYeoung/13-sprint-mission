package com.sprint.mission.discodeit.dto.response;

import java.util.UUID;

public record BinaryContentDownloadDto(
        UUID id,
        String originalFileName,
        String contentType,
        Long size,
        byte[] bytes
) {
}
