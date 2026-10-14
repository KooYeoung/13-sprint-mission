package com.sprint.mission.discodeit.security;

import com.sprint.mission.discodeit.events.UserCacheClearEvent;
import com.sprint.mission.discodeit.exception.jwt.TokenRenewalFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "discodeit.security.jwt",
        name = "registry-type",
        havingValue = "in-memory",
        matchIfMissing = true
)
public class InMemoryJwtRegistry implements JwtRegistry{

    private final Map<UUID , Queue<JwtInformation>> origin = new ConcurrentHashMap<>();

    private final JwtTokenProvider jwtTokenProvider;

    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        UUID userId = jwtInformation.userDto().id();
        Integer maxActiveJwtCount = jwtTokenProvider.getMaxActiveJwtCount();
        origin.compute(userId, (key, queue) -> {
            if (queue == null) {
                queue = new ConcurrentLinkedQueue<>();
            }

            while (queue.size() >= maxActiveJwtCount) {
                queue.poll();
            }
            queue.add(jwtInformation);
            return queue;
        });
    }

    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        origin.remove(userId);
    }

    @Override
    public void invalidateJwtInformationByRefreshToken(UUID userId, String refreshToken) {
        // 등록·로테이션과 같은 사용자별 잠금 안에서 비교와 삭제를 완료한다.
        origin.computeIfPresent(userId, (key, queue) -> {
            queue.removeIf(info -> info.refreshToken().equals(refreshToken));
            return queue.isEmpty() ? null : queue;
        });
    }

    @Override
    public boolean hasActiveJwtInformationByUserId(UUID userId) {
        Queue<JwtInformation> jwtInformations = origin.get(userId);
        if (jwtInformations == null) return false;

       return jwtInformations.stream()
               .anyMatch(jwtInformation -> jwtTokenProvider.validateToken(jwtInformation.refreshToken()).isPresent());
    }

    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        return origin.values().stream()
                .flatMap(info -> info.stream())
                .anyMatch(info -> info.accessToken().equals(accessToken));
    }

    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        return origin.values().stream()
                .flatMap(Collection::stream)
                .anyMatch(info -> info.refreshToken().equals(refreshToken));

    }

    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        UUID userId = newJwtInformation.userDto().id();
        Integer maxActiveJwtCount = jwtTokenProvider.getMaxActiveJwtCount();
        origin.compute(userId, (key, queue) -> {
            if (queue == null || queue.stream().noneMatch(info -> info.refreshToken().equals(refreshToken))) {
                throw new TokenRenewalFailedException();
            }
            // 검증과 교체를 사용자별로 원자적으로 처리하여 같은 토큰의 중복 갱신을 막는다.
            queue.removeIf(info -> info.refreshToken().equals(refreshToken));
            while (queue.size() >= maxActiveJwtCount) {
                queue.poll();
            }
            queue.add(newJwtInformation);
            return queue;
        });
    }

    @Override
    public Set<UUID> findActiveUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Set.of();

       return userIds.stream()
               .distinct()
               .filter(this::hasActiveJwtInformationByUserId)
               .collect(Collectors.toSet());
    }

    @Override
    public Set<UUID> findUserIdsWithValidAccessToken(Collection<UUID> userIds) {
        Set<UUID> onlineUserIds = new HashSet<>();
        Instant now = clock.instant();
        for (UUID userId : new HashSet<>(userIds)) {
            Queue<JwtInformation> tokens = origin.get(userId);
            if (tokens == null) continue;

            boolean hasValidAccessToken = tokens.stream()
                    // 만료 정리 작업 전이라도 만료된 세션은 제외한다.
                    .filter(info -> jwtTokenProvider.validateToken(info.refreshToken()).isPresent())
                    .flatMap(info -> jwtTokenProvider.validateToken(info.accessToken()).stream())
                    .filter(claims -> JwtTokenProvider.TokenType.ACCESS.name()
                            .equals(jwtTokenProvider.getTokenType(claims)))
                    .filter(claims -> userId.equals(jwtTokenProvider.getUserId(claims)))
                    .anyMatch(claims -> claims.getExpiration() != null
                            && claims.getExpiration().toInstant().isAfter(now));
            if (hasValidAccessToken) onlineUserIds.add(userId);
        }
        return onlineUserIds;
    }

    @Scheduled(fixedDelay = 1000 * 60 * 5)
    public void clearExpiredJwtInformation() {
        // 등록·로테이션과 같은 사용자별 잠금 안에서 검사와 삭제를 완료한다.
        Set<UUID> changedUserIds = new HashSet<>();
        origin.keySet().forEach(userId -> origin.computeIfPresent(userId, (key, queue) -> {
            boolean isRemoved = queue.removeIf(info -> jwtTokenProvider.validateToken(info.refreshToken()).isEmpty());
            if (isRemoved) {
                changedUserIds.add(userId);
            }
            return queue.isEmpty() ? null : queue;
        }));

        changedUserIds.forEach(userId -> eventPublisher.publishEvent(new UserCacheClearEvent(userId)));
    }

}
