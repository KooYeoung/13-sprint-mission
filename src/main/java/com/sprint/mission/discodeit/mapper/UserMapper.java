package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final BinaryContentMapper binaryContentMapper;

    public UserDto toDto(User user) {
        return toDto(user, user.isOnline());
    }

    public UserDto toDto(User user, boolean isOnline) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                binaryContentMapper.toDto(user.getProfile()),
                isOnline,
                RequestTimeZoneUtils.toOffsetDateTime(user.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(user.getUpdatedAt())
        );
    }


}
