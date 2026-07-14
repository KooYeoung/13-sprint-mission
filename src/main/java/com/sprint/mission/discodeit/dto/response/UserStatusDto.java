package com.sprint.mission.discodeit.dto.response;

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

    // 마지막 접속 시간이 현재 시간으로부터 5분 이내이면 현재 접속 중인 유저
    public boolean isOnline() {
        if (lastActiveAt == null) return false;

        return lastActiveAt
                .plus(Duration.ofMinutes(5))
                .isAfter(OffsetDateTime.now());
    }

}