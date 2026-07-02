package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.DownloadDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class BinaryContentMapper {

    public BinaryContentDto toDto(BinaryContent binaryContent) {
        return new BinaryContentDto(
                binaryContent.getId(),
                binaryContent.getOriginalFileName(),
                binaryContent.getSize(),
                binaryContent.getContentType()
        );
    }

    public DownloadDto toDto(BinaryContent binaryContent, Resource resource) {
        return new DownloadDto(toDto(binaryContent), resource);
    }

}
