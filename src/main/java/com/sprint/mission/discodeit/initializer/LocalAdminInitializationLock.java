package com.sprint.mission.discodeit.initializer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "spring.cache", name = "type", havingValue = "caffeine")
public class LocalAdminInitializationLock implements AdminInitializationLock {
    // 단일 애플리케이션 인스턴스 안에서만 공유하며, 만료나 자동 해제는 하지 않는다.
    private final AtomicReference<String> owner = new AtomicReference<>();

    @Override
    public Optional<String> tryAcquire() {
        String ownerToken = UUID.randomUUID().toString();
        return owner.compareAndSet(null, ownerToken) ? Optional.of(ownerToken) : Optional.empty();
    }

    @Override
    public void release(String ownerToken) {
        String currentOwner = owner.get();
        // 토큰 값으로 소유자를 확인하고, 확인 이후 소유자가 바뀌었으면 해제하지 않는다.
        if (currentOwner == null || !currentOwner.equals(ownerToken) || !owner.compareAndSet(currentOwner, null)) {
            log.warn("관리자 초기화 로컬 락이 이미 해제되었거나 소유자가 달라 해제를 생략합니다.");
        }
    }
}
