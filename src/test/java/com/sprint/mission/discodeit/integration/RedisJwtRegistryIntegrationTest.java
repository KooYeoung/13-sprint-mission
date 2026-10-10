package com.sprint.mission.discodeit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.config.CacheConfig;
import com.sprint.mission.discodeit.config.JwtProperties;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.jwt.TokenRenewalFailedException;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.JwtInformation;
import com.sprint.mission.discodeit.security.JwtRegistry;
import com.sprint.mission.discodeit.security.JwtTokenProvider;
import com.sprint.mission.discodeit.security.RedisJwtRegistry;
import com.sprint.mission.discodeit.service.basic.UserReader;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:redis-jwt;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.cache.type=redis",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=off",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=off",
        "discodeit.security.jwt.registry-type=redis",
        "discodeit.security.jwt.max-active-jwt-count=2",
        "discodeit.security.jwt.secret=redis-jwt-integration-test-secret-at-least-32-bytes",
        "discodeit.security.jwt.issuer=redis-jwt-test",
        "discodeit.security.jwt.access-token-validity=10m",
        "discodeit.security.jwt.refresh-token-validity=60s",
        "discodeit.storage.type=local"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("실제 Redis의 JWT Lua 처리 및 온라인 상태 통합 테스트")
class RedisJwtRegistryIntegrationTest {
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry properties) {
        // 애플리케이션 컨텍스트 생성 전에 기동한다. 개발용 Redis에는 연결하지 않는다.
        REDIS.start();
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @AfterAll
    static void stopRedis() {
        REDIS.stop();
    }

    @Autowired RedisJwtRegistry registry;
    @Autowired JwtTokenProvider tokens;
    @Autowired JwtProperties jwtProperties;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired Clock clock;
    @Autowired UserRepository users;
    @Autowired UserReader userReader;
    @Autowired CacheManager cacheManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired ApplicationContext context;

    private LettuceConnectionFactory secondConnection;
    private StringRedisTemplate secondTemplate;
    private RedisJwtRegistry secondRegistry;
    private final Set<UUID> tokenUserIds = new HashSet<>();
    private final List<UUID> persistedUserIds = new ArrayList<>();

    @BeforeEach
    void connectSecondInstance() {
        // 별도 클라이언트 연결을 사용해 인스턴스 메모리를 공유하지 않고 Redis로 상태가 전달되는지 검증한다.
        secondConnection = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        secondConnection.afterPropertiesSet();
        secondConnection.start();
        secondTemplate = new StringRedisTemplate(secondConnection);
        secondRegistry = new RedisJwtRegistry(tokens, secondTemplate, objectMapper, clock);
    }

    @AfterEach
    void cleanFixtures() {
        try {
            tokenUserIds.forEach(registry::invalidateJwtInformationByUserId);
            persistedUserIds.forEach(id -> userCache().evict(id));
            users.deleteAllById(persistedUserIds);
        } finally {
            secondConnection.destroy();
        }
    }

    @ParameterizedTest(name = "최대 세션 수 {0}")
    @ValueSource(ints = {1, 2})
    @DisplayName("설정한 세션 수까지만 유지하고 가장 오래된 세션을 제거한다")
    void register_respectsLimitAcrossInstances(int limit) {
        assertThat(context.getBeansOfType(JwtRegistry.class).values()).containsExactly(registry);
        JwtTokenProvider limitedTokens = provider(limit, Duration.ofSeconds(60));
        RedisJwtRegistry first = new RedisJwtRegistry(limitedTokens, redisTemplate, objectMapper, clock);
        RedisJwtRegistry second = new RedisJwtRegistry(limitedTokens, secondTemplate, objectMapper, clock);
        UserDto user = principal(identified(user())).getUserDto();
        List<JwtInformation> sessions = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            JwtInformation session = information(user, limitedTokens);
            first.registerJwtInformation(session);
            sessions.add(session);
        }
        sessions.forEach(session -> assertTokensActive(second, session, true));

        JwtInformation newest = information(user, limitedTokens);
        second.registerJwtInformation(newest);

        assertTokensActive(first, sessions.get(0), false);
        sessions.subList(1, sessions.size()).forEach(session -> assertTokensActive(first, session, true));
        assertTokensActive(first, newest, true);
    }

    @Test
    @DisplayName("갱신한 세션은 가장 최근 순서로 이동하고 다음 로그인에도 유지된다")
    void rotate_movesSessionToEnd() {
        UserDto user = principal(identified(user())).getUserDto();
        JwtInformation oldest = information(user, tokens);
        JwtInformation other = information(user, tokens);
        JwtInformation rotated = information(user, tokens);
        JwtInformation newest = information(user, tokens);
        registry.registerJwtInformation(oldest);
        registry.registerJwtInformation(other);

        secondRegistry.rotateJwtInformation(oldest.refreshToken(), rotated);
        registry.registerJwtInformation(newest);

        assertTokensActive(secondRegistry, oldest, false);
        assertTokensActive(secondRegistry, other, false);
        assertTokensActive(secondRegistry, rotated, true);
        assertTokensActive(secondRegistry, newest, true);
    }

    @Test
    @DisplayName("두 인스턴스에서 같은 Refresh Token을 동시에 갱신하면 하나만 성공한다")
    void rotate_allowsExactlyOneConcurrentReplacement() throws Exception {
        UserDto user = principal(identified(user())).getUserDto();
        JwtInformation old = information(user, tokens);
        JwtInformation first = information(user, tokens);
        JwtInformation second = information(user, tokens);
        registry.registerJwtInformation(old);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstResult = executor.submit(() -> rotateAfterSignal(registry, old, first, ready, start));
            Future<Boolean> secondResult = executor.submit(() -> rotateAfterSignal(secondRegistry, old, second, ready, start));
            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();
            boolean firstSucceeded = firstResult.get(5, SECONDS);
            boolean secondSucceeded = secondResult.get(5, SECONDS);

            assertThat(firstSucceeded).isNotEqualTo(secondSucceeded);
            assertTokensActive(registry, old, false);
            assertTokensActive(registry, first, firstSucceeded);
            assertTokensActive(secondRegistry, second, secondSucceeded);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("이전 토큰의 로그아웃은 갱신된 세션과 다른 세션을 유지하고 마지막 로그아웃은 키를 삭제한다")
    void logout_preservesOtherSessionsAndRemovesLastKey() {
        UserDto user = principal(identified(user())).getUserDto();
        JwtInformation old = information(user, tokens);
        JwtInformation other = information(user, tokens);
        JwtInformation current = information(user, tokens);
        registry.registerJwtInformation(old);
        registry.registerJwtInformation(other);
        registry.rotateJwtInformation(old.refreshToken(), current);

        secondRegistry.invalidateJwtInformationByRefreshToken(user.id(), old.refreshToken());
        assertTokensActive(registry, current, true);
        assertTokensActive(registry, other, true);

        secondRegistry.invalidateJwtInformationByRefreshToken(user.id(), current.refreshToken());
        assertTokensActive(registry, current, false);
        assertTokensActive(registry, other, true);
        assertThat(registry.hasActiveJwtInformationByUserId(user.id())).isTrue();

        secondRegistry.invalidateJwtInformationByRefreshToken(user.id(), other.refreshToken());
        assertThat(redisTemplate.hasKey(jwtKey(user.id()))).isFalse();
        assertThat(registry.hasActiveJwtInformationByUserId(user.id())).isFalse();
        secondRegistry.invalidateJwtInformationByRefreshToken(user.id(), other.refreshToken());
        assertThatThrownBy(() -> registry.rotateJwtInformation(other.refreshToken(), current))
                .isInstanceOf(TokenRenewalFailedException.class);
        assertThat(redisTemplate.hasKey(jwtKey(user.id()))).isFalse();
    }

    @Test
    @DisplayName("전체 무효화는 다른 인스턴스에 반영되고 이전 로그인 토큰으로 새 로그인을 삭제할 수 없다")
    void invalidateAll_andStaleLogoutAreVisibleAcrossInstances() {
        UserDto user = principal(identified(user())).getUserDto();
        JwtInformation old = information(user, tokens);
        JwtInformation current = information(user, tokens);
        registry.registerJwtInformation(old);

        secondRegistry.invalidateJwtInformationByUserId(user.id());
        assertTokensActive(registry, old, false);
        assertThat(registry.hasActiveJwtInformationByUserId(user.id())).isFalse();
        registry.registerJwtInformation(current);
        secondRegistry.invalidateJwtInformationByRefreshToken(user.id(), old.refreshToken());

        assertTokensActive(registry, current, true);
        assertThat(secondRegistry.hasActiveJwtInformationByUserId(user.id())).isTrue();
    }

    @Test
    @DisplayName("파이프라인은 누락·중복 ID가 섞여도 EXISTS 결과를 요청한 사용자와 정확히 대응한다")
    void findActiveUserIds_handlesMissingAndDuplicateIdsInCommandOrder() {
        JwtInformation first = information(principal(identified(user())).getUserDto(), tokens);
        JwtInformation second = information(principal(identified(user())).getUserDto(), tokens);
        registry.registerJwtInformation(first);
        registry.registerJwtInformation(second);

        assertThat(secondRegistry.findActiveUserIds(List.of())).isEmpty();
        assertThat(secondRegistry.findActiveUserIds(List.of(UUID.randomUUID(), first.userDto().id(),
                UUID.randomUUID(), second.userDto().id(), first.userDto().id(), UUID.randomUUID())))
                .containsExactlyInAnyOrder(first.userDto().id(), second.userDto().id());
    }

    @Test
    @DisplayName("가장 늦은 Refresh Token 만료 시각으로 TTL을 정하고 해당 세션 삭제 시 앞당긴다")
    void logout_recalculatesTtlFromRemainingSessions() {
        UserDto user = principal(identified(user())).getUserDto();
        JwtInformation longer = information(user, tokens);
        JwtInformation shorter = information(user, provider(2, Duration.ofSeconds(20)));
        registry.registerJwtInformation(longer);
        // 마지막에 등록된 세션의 만료가 더 빠르더라도 최댓값을 사용해야 한다.
        registry.registerJwtInformation(shorter);
        assertExpiresWith(user.id(), longer);

        secondRegistry.invalidateJwtInformationByRefreshToken(user.id(), longer.refreshToken());

        assertTokensActive(registry, shorter, true);
        assertExpiresWith(user.id(), shorter);
    }

    @Test
    @DisplayName("Redis JWT가 자연 만료되면 사용자 캐시가 남아 있어도 단건·일괄 조회는 오프라인이다")
    void userReader_recalculatesOnlineAfterNaturalExpiryWithoutReloadingDatabase() {
        User user = users.saveAndFlush(user());
        persistedUserIds.add(user.getId());
        // Redis TIME도 실제로 흐르므로 과거 고정 Clock 대신 현재 시각으로 짧은 토큰을 발급한다.
        JwtInformation session = information(principal(user).getUserDto(), provider(2, Duration.ofSeconds(3)));
        registry.registerJwtInformation(session);
        assertThat(userReader.findUserDto(user.getId()).orElseThrow().online()).isTrue();
        assertThat(userReader.findUserDtosByIds(List.of(user.getId())).get(user.getId()).online()).isTrue();
        UserDto cached = userCache().get(user.getId(), UserDto.class);
        assertThat(cached).isNotNull();
        assertThat(cached.online()).isFalse();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        await().pollInterval(Duration.ofMillis(100)).atMost(Duration.ofSeconds(10))
                .until(() -> !secondRegistry.hasActiveJwtInformationByUserId(user.getId()));

        assertThat(userReader.findUserDto(user.getId()).orElseThrow().online()).isFalse();
        assertThat(userReader.findUserDtosByIds(List.of(user.getId())).get(user.getId()).online()).isFalse();
        assertThat(userCache().get(user.getId(), UserDto.class)).isEqualTo(cached);
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    private JwtTokenProvider provider(int limit, Duration refreshValidity) {
        return new JwtTokenProvider(new JwtProperties(limit, jwtProperties.secret(),
                jwtProperties.accessTokenValidity(), refreshValidity, jwtProperties.issuer()), clock);
    }

    private JwtInformation information(UserDto user, JwtTokenProvider issuer) {
        tokenUserIds.add(user.id());
        return new JwtInformation(user, issuer.generateAccessToken(user.id(), user.username(), user.role()),
                issuer.generateRefreshToken(user.id(), user.username()));
    }

    private void assertTokensActive(JwtRegistry reader, JwtInformation session, boolean active) {
        assertThat(reader.hasActiveJwtInformationByAccessToken(session.accessToken())).isEqualTo(active);
        assertThat(reader.hasActiveJwtInformationByRefreshToken(session.refreshToken())).isEqualTo(active);
    }

    private void assertExpiresWith(UUID userId, JwtInformation session) {
        long expected = tokens.parseClaims(session.refreshToken()).getExpiration().getTime();
        Long remaining = redisTemplate.getExpire(jwtKey(userId), MILLISECONDS);
        assertThat(remaining).isNotNull().isPositive();
        // TTL 조회와 JVM 시각 확인 사이의 실행 지연을 허용하되 서로 다른 만료 시각은 구분한다.
        assertThat(clock.millis() + remaining).isCloseTo(expected, within(1000L));
    }

    private Cache userCache() {
        return Objects.requireNonNull(cacheManager.getCache(CacheConfig.USERS));
    }

    private String jwtKey(UUID userId) {
        return "discodeit:jwt:" + userId;
    }

    private boolean rotateAfterSignal(RedisJwtRegistry target, JwtInformation old, JwtInformation replacement,
                                      CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertThat(start.await(5, SECONDS)).isTrue();
        try {
            target.rotateJwtInformation(old.refreshToken(), replacement);
            return true;
        } catch (TokenRenewalFailedException e) {
            return false;
        }
    }
}
