package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        config = MapStructConfig.class,
        uses = {DateTimeMapper.class}
)
public interface UserStatusMapper {

    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "lastActiveAt", target = "lastActiveAt", qualifiedByName = "toOffsetDateTime")
    UserStatusDto toDto(UserStatus userStatus);
}
