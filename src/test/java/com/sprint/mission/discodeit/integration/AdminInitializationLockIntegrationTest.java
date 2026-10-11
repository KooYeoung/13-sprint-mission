package com.sprint.mission.discodeit.integration;

import com.sprint.mission.discodeit.config.AdminUserProperties;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.events.RoleUpdatedEvent;
import com.sprint.mission.discodeit.initializer.AdminUserInitializer;
import com.sprint.mission.discodeit.initializer.RedisAdminInitializationLock;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED;
import static org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-initialization-lock;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.cache.type=redis",
        "discodeit.security.jwt.registry-type=in-memory",
        "discodeit.async.enabled=false",
        "discodeit.storage.type=local"
})
@ActiveProfiles("test")
@Import(AdminInitializationLockIntegrationTest.CompletionProbeConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("관리자 초기화의 Redis 락 및 트랜잭션 통합 테스트")
class AdminInitializationLockIntegrationTest {
    private static final String LOCK_KEY = "discodeit:lock:admin-initialization";
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry properties) {
        // 앱 기동 시의 관리자 초기화도 격리된 실제 Redis에 연결한다.
        REDIS.start();
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @AfterAll
    static void stopRedis() {
        REDIS.stop();
    }

    @Autowired AdminUserInitializer initializer;
    @Autowired AdminUserProperties adminProperties;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired UserRepository users;
    @Autowired NotificationRepository notifications;
    @Autowired CompletionProbe probe;
    // 실제 해제를 수행하면서 그 시점의 DB 커밋/롤백 상태도 관찰하기 위해 Spy를 사용한다.
    @MockitoSpyBean RedisAdminInitializationLock lock;

    private LettuceConnectionFactory secondConnection;
    private RedisAdminInitializationLock secondLock;

    @BeforeEach
    void prepareIsolatedState() {
        probe.reset();
        // 이 클래스 전용 H2 DB의 기동 시 생성된 관리자와 알림을 정리한다.
        notifications.deleteAllInBatch();
        users.deleteAllInBatch();
        redisTemplate.delete(LOCK_KEY);
        secondConnection = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        secondConnection.afterPropertiesSet();
        secondConnection.start();
        secondLock = new RedisAdminInitializationLock(new StringRedisTemplate(secondConnection));
        clearInvocations(lock); // 앱 기동 시 초기화 호출은 각 테스트의 검증에서 제외한다.
    }

    @AfterEach
    void closeSecondConnection() {
        try {
            redisTemplate.delete(LOCK_KEY);
        } finally {
            secondConnection.destroy();
        }
    }

    @Test
    @DisplayName("두 연결의 동시 획득은 하나만 성공하고 일치하는 소유자만 락을 해제한다")
    void tryAcquire_allowsOneOwnerAcrossConnections() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        String owner;
        try {
            Future<Optional<String>> first = executor.submit(() -> acquireAfterSignal(lock, ready, start));
            Future<Optional<String>> second = executor.submit(() -> acquireAfterSignal(secondLock, ready, start));
            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();
            Optional<String> firstResult = first.get(5, SECONDS);
            Optional<String> secondResult = second.get(5, SECONDS);
            assertThat(firstResult.isPresent()).isNotEqualTo(secondResult.isPresent());
            owner = firstResult.or(() -> secondResult).orElseThrow();
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }

        assertThat(redisTemplate.getExpire(LOCK_KEY, MILLISECONDS)).isBetween(1L, 60_000L);
        secondLock.release(UUID.randomUUID().toString());
        assertThat(redisTemplate.opsForValue().get(LOCK_KEY)).isEqualTo(owner);
        secondLock.release(owner);
        assertThat(redisTemplate.hasKey(LOCK_KEY)).isFalse();
        String newOwner = lock.tryAcquire().orElseThrow();
        assertThat(newOwner).isNotEqualTo(owner);
        lock.release(newOwner);
    }

    @Test
    @DisplayName("만료 후 재획득한 락을 이전 소유자가 해제할 수 없다")
    void release_preservesNewOwnerAfterExpiry() {
        String oldOwner = lock.tryAcquire().orElseThrow();
        // 60초를 기다리는 대신 테스트 키의 만료만 앞당겨 실제 Redis 만료를 검증한다.
        redisTemplate.expire(LOCK_KEY, Duration.ofMillis(100));
        await().atMost(Duration.ofSeconds(5)).until(() -> !redisTemplate.hasKey(LOCK_KEY));
        lock.release(oldOwner); // 이미 만료된 락의 해제도 안전하다.
        String newOwner = secondLock.tryAcquire().orElseThrow();

        lock.release(oldOwner);

        assertThat(redisTemplate.opsForValue().get(LOCK_KEY)).isEqualTo(newOwner);
        secondLock.release(newOwner);
        assertThat(redisTemplate.hasKey(LOCK_KEY)).isFalse();
    }

    @ParameterizedTest(name = "생성 중 예외 발생: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("실제 관리자 생성 트랜잭션이 커밋 또는 롤백된 뒤 락을 해제한다")
    void run_releasesOnlyAfterTransactionCompletion(boolean failCreation) {
        probe.armed = true;
        probe.failCreation = failCreation;
        doAnswer(invocation -> {
            assertThat(probe.completionStatus).isEqualTo(failCreation ? STATUS_ROLLED_BACK : STATUS_COMMITTED);
            assertThat(probe.lockHeldAtCompletion).isTrue();
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            // 초기화 트랜잭션 밖에서 실제 DB를 재조회하여 저장/롤백 결과를 확인한다.
            var admin = users.findByUsername(adminProperties.username());
            if (failCreation) {
                assertThat(admin).isEmpty();
            } else {
                assertThat(admin).isPresent();
                assertThat(admin.orElseThrow().getRole()).isEqualTo(Role.ADMIN);
            }
            invocation.callRealMethod();
            return null;
        }).when(lock).release(anyString());

        if (failCreation) {
            assertThatThrownBy(() -> initializer.run(new DefaultApplicationArguments()))
                    .isSameAs(probe.failure);
        } else {
            initializer.run(new DefaultApplicationArguments());
        }

        verify(lock).release(anyString());
        assertThat(redisTemplate.hasKey(LOCK_KEY)).isFalse();
    }

    private Optional<String> acquireAfterSignal(RedisAdminInitializationLock target,
                                              CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertThat(start.await(5, SECONDS)).isTrue();
        return target.tryAcquire();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CompletionProbeConfig {
        @Bean
        CompletionProbe completionProbe(StringRedisTemplate redisTemplate) {
            return new CompletionProbe(redisTemplate);
        }
    }

    static class CompletionProbe {
        private final StringRedisTemplate redisTemplate;
        private final RuntimeException failure = new IllegalStateException("관리자 권한 변경 중 테스트 오류");
        private boolean armed;
        private boolean failCreation;
        private int completionStatus = -1;
        private boolean lockHeldAtCompletion;

        CompletionProbe(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        void reset() {
            armed = false;
            failCreation = false;
            completionStatus = -1;
            lockHeldAtCompletion = false;
        }

        @EventListener
        public void onRoleUpdated(RoleUpdatedEvent event) {
            if (!armed) {
                return;
            }
            // 사용자가 저장되고 권한을 변경하는 실제 트랜잭션 안에서 완료 시점을 기록한다.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    completionStatus = status;
                    lockHeldAtCompletion = Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY));
                }
            });
            if (failCreation) {
                throw failure;
            }
        }
    }
}
