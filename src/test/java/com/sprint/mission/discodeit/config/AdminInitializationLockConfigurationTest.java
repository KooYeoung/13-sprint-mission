package com.sprint.mission.discodeit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.initializer.AdminInitializationLock;
import com.sprint.mission.discodeit.initializer.AdminUserInitializer;
import com.sprint.mission.discodeit.initializer.LocalAdminInitializationLock;
import com.sprint.mission.discodeit.initializer.RedisAdminInitializationLock;
import com.sprint.mission.discodeit.service.AdminUserService;
import com.sprint.mission.discodeit.service.UserRoleQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("캐시 타입에 따른 관리자 초기화 락 빈 선택 테스트")
class AdminInitializationLockConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CacheConfig.class, LocalAdminInitializationLock.class, RedisAdminInitializationLock.class)
            // 이 테스트는 락과 직렬화 설정의 빈 선택만 검증한다. 캐시 매니저 선택은 기동 테스트에서 확인한다.
            .withBean(CacheManager.class, CaffeineCacheManager::new);

    @Test
    @DisplayName("Caffeine은 Redis 의존 객체 없이 로컬 락만 등록한다")
    void caffeine_registersLocalLockWithoutRedisDependencies() {
        runner.withPropertyValues("spring.cache.type=caffeine").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AdminInitializationLock.class)
                    .hasSingleBean(LocalAdminInitializationLock.class)
                    .doesNotHaveBean(RedisAdminInitializationLock.class)
                    .doesNotHaveBean(RedisCacheConfiguration.class)
                    .doesNotHaveBean(StringRedisTemplate.class);
        });
    }

    @ParameterizedTest(name = "캐시 설정: {0}")
    @ValueSource(strings = {"redis", ""})
    @DisplayName("Redis를 선택하거나 타입을 생략하면 Redis 락과 직렬화 설정만 등록한다")
    void redisOrMissingType_registersRedisLockAndSerialization(String type) {
        ApplicationContextRunner configured = runner
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean(ObjectMapper.class, ObjectMapper::new);
        if (!type.isEmpty()) {
            configured = configured.withPropertyValues("spring.cache.type=" + type);
        }
        configured.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AdminInitializationLock.class)
                    .hasSingleBean(RedisAdminInitializationLock.class)
                    .hasSingleBean(RedisCacheConfiguration.class)
                    .doesNotHaveBean(LocalAdminInitializationLock.class);
        });
    }

    @Test
    @DisplayName("지원하지 않는 캐시 타입은 로컬 락으로 대체하지 않고 초기화 빈 생성에 실패한다")
    void unsupportedType_doesNotFallBackToLocalLock() {
        runner.withPropertyValues("spring.cache.type=none")
                .withUserConfiguration(AdminUserInitializer.class)
                .withBean(AdminUserProperties.class, () -> new AdminUserProperties("admin", "password", "admin@example.com"))
                .withBean(UserRoleQueryService.class, () -> mock(UserRoleQueryService.class))
                .withBean(AdminUserService.class, () -> mock(AdminUserService.class))
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class)
                        .hasStackTraceContaining(AdminInitializationLock.class.getName()));
    }
}
