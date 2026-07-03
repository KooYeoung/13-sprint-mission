package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.DownloadDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.io.Resource;

@Mapper(config = MapStructConfig.class)
public interface BinaryContentMapper {

    @Mapping(source = "originalFileName", target = "fileName")
    BinaryContentDto toDto(BinaryContent binaryContent);

    default DownloadDto toDownloadDto(BinaryContent binaryContent, Resource resource) {
        return new DownloadDto(toDto(binaryContent), resource);
    }

}
