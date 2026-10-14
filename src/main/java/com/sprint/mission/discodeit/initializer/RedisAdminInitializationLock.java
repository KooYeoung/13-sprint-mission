package com.sprint.mission.discodeit.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "spring.cache", name = "type", havingValue = "redis", matchIfMissing = true)
public class RedisAdminInitializationLock implements AdminInitializationLock {
    private static final String LOCK_KEY = "discodeit:lock:admin-initialization";
    // 자동 연장은 하지 않으므로 관리자 초기화는 이 시간 안에 완료되어야 한다.
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);
    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/release-admin-initialization-lock.lua"), Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<String> tryAcquire() {
        String ownerToken = UUID.randomUUID().toString();
        try {
            // 키 생성과 만료 설정을 한 번에 수행한다. 이미 잠겨 있으면 기다리지 않는다.
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, ownerToken, LOCK_TTL);
            if (acquired == null) {
                throw new IllegalStateException("관리자 초기화 락 획득 결과가 없습니다.");
            }
            return acquired ? Optional.of(ownerToken) : Optional.empty();
        } catch (RuntimeException e) {
            log.error("관리자 초기화 락 획득에 실패했습니다. key={}", LOCK_KEY, e);
            throw e;
        }
    }

    @Override
    public void release(String ownerToken) {
        try {
            Long released = redisTemplate.execute(RELEASE_SCRIPT, List.of(LOCK_KEY), ownerToken);
            if (released == null || (released != 0L && released != 1L)) {
                throw new IllegalStateException("관리자 초기화 락 해제 결과가 올바르지 않습니다.");
            }
            if (released == 0L) {
                log.warn("관리자 초기화 락이 만료되었거나 소유자가 달라 해제를 생략합니다. key={}", LOCK_KEY);
            }
        } catch (RuntimeException e) {
            log.error("관리자 초기화 락 해제에 실패했습니다. key={}", LOCK_KEY, e);
            throw e;
        }
    }
}
