package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        config = MapStructConfig.class,
        uses = {BinaryContentMapper.class, DateTimeMapper.class}
)
public interface UserMapper {

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.profile", target = "profile")
    @Mapping(source = "online", target = "online")
    @Mapping(source = "user.createdAt", target = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "user.updatedAt", target = "updatedAt", qualifiedByName = "toOffsetDateTime")
    UserDto toDto(User user, boolean online);

    default UserDto toDto(User user) {
        if (user == null) return null;

        return toDto(user, user.isOnline());
    }


}
