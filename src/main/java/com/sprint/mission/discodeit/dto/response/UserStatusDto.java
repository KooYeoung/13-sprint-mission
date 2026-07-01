package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserStatusDto(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID userId,
        OffsetDateTime lastActiveAt,
        boolean online
) {
    public static UserStatusDto from(UserStatus userStatus) {
        return new UserStatusDto(
                userStatus.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getUpdatedAt()),
                userStatus.getUser().getId(),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getLastActiveAt()),
                userStatus.isOnline()
        );
    }
}