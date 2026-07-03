package com.sprint.mission.discodeit.dto.response;

import org.springframework.core.io.Resource;

public record DownloadDto(
        BinaryContentDto binaryContent,
        Resource resource
) {


}
