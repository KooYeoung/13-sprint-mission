package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import org.springframework.stereotype.Component;

@Component
public class UserStatusMapper {

    public UserStatusDto toDto(UserStatus userStatus) {
        return new UserStatusDto(
                userStatus.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getUpdatedAt()),
                userStatus.getUserId(),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getLastActiveAt())
        );
    }
}
