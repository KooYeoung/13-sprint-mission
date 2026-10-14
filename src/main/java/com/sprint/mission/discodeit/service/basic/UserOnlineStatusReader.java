package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.security.JwtRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 등록된 유효한 Access Token의 존재 여부로 온라인 표시를 판단한다.
 * 실제 요청 활동 여부를 나타내지는 않는다.
 */
@Component
@RequiredArgsConstructor
public class UserOnlineStatusReader {

    private final JwtRegistry jwtRegistry;

    public boolean isOnline(UUID userId) {
        return findOnlineUserIds(List.of(userId)).contains(userId);
    }

    public Set<UUID> findOnlineUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Set.of();

        return jwtRegistry.findUserIdsWithValidAccessToken(userIds);
    }
}
