package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.DownloadDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.storage.DownloadResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.io.Resource;

@Mapper(config = MapStructConfig.class)
public interface BinaryContentMapper {

    @Mapping(source = "originalFileName", target = "fileName")
    BinaryContentDto toDto(BinaryContent binaryContent);

    default DownloadDto toDownloadDto(BinaryContent binaryContent, DownloadResult result) {
        return new DownloadDto(toDto(binaryContent), result);
    }

}
