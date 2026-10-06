package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

@Mapper(
        config = MapStructConfig.class,
        uses = {BinaryContentMapper.class, DateTimeMapper.class, UserOnlineMapper.class}
)
public interface UserMapper {

    @Named("userDtoToRequestTimeZone")
    static UserDto toRequestTimeZone(UserDto user) {
        var createdAt = RequestTimeZoneUtils.toOffsetDateTime(user.createdAt() == null ? null : user.createdAt().toInstant());
        var updatedAt = RequestTimeZoneUtils.toOffsetDateTime(user.updatedAt() == null ? null : user.updatedAt().toInstant());
        return new UserDto(user.id(), user.username(), user.email(), user.profile(), user.online(), user.role(), createdAt, updatedAt);
    }

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.profile", target = "profile")
    @Mapping(source = "user.id", target = "online", qualifiedByName = "isOnline")
    @Mapping(source = "user.role", target = "role")
    @Mapping(source = "user.createdAt", target = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "user.updatedAt", target = "updatedAt", qualifiedByName = "toOffsetDateTime")
    UserDto toDto(User user);

    @InheritConfiguration(name = "toDto")
    @Mapping(source = "online", target = "online")
    @Mapping(source = "user.createdAt", target = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "user.updatedAt", target = "updatedAt", qualifiedByName = "toOffsetDateTime")
    UserDto toDto(User user, boolean online);

}
