package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserStatusDto(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID userId,
        OffsetDateTime lastActiveAt
) {
    public static UserStatusDto from(UserStatus userStatus) {
        return new UserStatusDto(
                userStatus.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getUpdatedAt()),
                userStatus.getUser().getId(),
                RequestTimeZoneUtils.toOffsetDateTime(userStatus.getLastActiveAt())
        );
    }

    // 마지막 접속 시간이 현재 시간으로부터 5분 이내이면 현재 접속 중인 유저
    public boolean isOnline() {
        if (lastActiveAt == null) return false;

        return lastActiveAt
                .plus(Duration.ofMinutes(5))
                .isAfter(OffsetDateTime.now());
    }

}