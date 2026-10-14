package com.sprint.mission.discodeit.initializer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("관리자 초기화 로컬 락 단위 테스트")
class LocalAdminInitializationLockTest {
    private final LocalAdminInitializationLock lock = new LocalAdminInitializationLock();

    @Test
    @DisplayName("토큰 값이 일치할 때만 해제하고 이전 소유자는 새 락을 해제할 수 없다")
    void release_requiresCurrentOwnerAndAllowsReacquisition() {
        String owner = lock.tryAcquire().orElseThrow();
        assertThat(lock.tryAcquire()).isEmpty();

        lock.release(UUID.randomUUID().toString());
        assertThat(lock.tryAcquire()).isEmpty();

        // 같은 String 객체인지가 아니라 소유자 토큰의 값이 같은지 확인해야 한다.
        lock.release(new String(owner));
        String newOwner = lock.tryAcquire().orElseThrow();
        assertThat(newOwner).isNotEqualTo(owner);

        lock.release(owner);
        assertThat(lock.tryAcquire()).isEmpty();
        lock.release(newOwner);
        assertThat(lock.tryAcquire()).isPresent();
    }

    @Test
    @DisplayName("같은 인스턴스의 두 스레드가 동시에 획득을 시도하면 하나만 성공한다")
    void tryAcquire_allowsOnlyOneConcurrentOwner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Optional<String>> first = executor.submit(() -> acquireAfterSignal(ready, start));
            Future<Optional<String>> second = executor.submit(() -> acquireAfterSignal(ready, start));
            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();
            Optional<String> firstResult = first.get(5, SECONDS);
            Optional<String> secondResult = second.get(5, SECONDS);

            assertThat(firstResult.isPresent()).isNotEqualTo(secondResult.isPresent());
            lock.release(firstResult.or(() -> secondResult).orElseThrow());
            assertThat(lock.tryAcquire()).isPresent();
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    private Optional<String> acquireAfterSignal(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertThat(start.await(5, SECONDS)).isTrue();
        return lock.tryAcquire();
    }
}
