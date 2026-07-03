package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.entity.ReadStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        config = MapStructConfig.class,
        uses = {DateTimeMapper.class}
)
public interface ReadStatusMapper {

    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "lastReadAt", target = "lastReadAt", qualifiedByName = "toOffsetDateTime")
    ReadStatusDto toDto(ReadStatus readStatus);
}
