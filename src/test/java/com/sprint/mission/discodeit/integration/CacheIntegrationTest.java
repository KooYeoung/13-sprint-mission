package com.sprint.mission.discodeit.integration;

import com.sprint.mission.discodeit.config.CacheConfig;
import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserRoleUpdateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.repository.*;
import com.sprint.mission.discodeit.security.*;
import com.sprint.mission.discodeit.security.handler.JwtLoginSuccessHandler;
import com.sprint.mission.discodeit.security.handler.JwtLogoutHandler;
import com.sprint.mission.discodeit.service.*;
import com.sprint.mission.discodeit.service.basic.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cache-integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=off",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=off",
        "discodeit.async.enabled=false",
        "discodeit.storage.type=local",
        "management.endpoints.web.exposure.include=health,caches,metrics"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CacheIntegrationTest {
    @Autowired ChannelService channelService;
    @Autowired UserService userService;
    @Autowired MessageService messageService;
    @Autowired NotificationService notificationService;
    @Autowired UserRoleManager roles;
    @Autowired ChannelReader channelReader;
    @Autowired UserReader userReader;
    @Autowired ChannelRepository channels;
    @Autowired UserRepository users;
    @Autowired BinaryContentRepository binaryContents;
    @Autowired ReadStatusRepository readStatuses;
    @Autowired MessageRepository messages;
    @Autowired NotificationRepository notifications;
    @Autowired CacheManager cacheManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JwtRegistry jwtRegistry;
    @Autowired JwtTokenProvider tokens;
    @Autowired JwtLoginSuccessHandler loginHandler;
    @Autowired JwtLogoutHandler logoutHandler;
    @Autowired MockMvc mvc;

    TransactionTemplate transaction;
    Statistics statistics;
    User owner;
    User manager;
    Channel first;
    Channel second;

    @BeforeEach
    void setUp() {
        clearCaches();
        transaction = new TransactionTemplate(transactionManager);
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        transaction.executeWithoutResult(status -> {
            owner = users.save(user());
            manager = user();
            manager.updateRole(new UserRoleUpdateCommand(Role.ADMIN));
            manager = users.save(manager);
            first = channels.save(channel(ChannelType.PUBLIC));
            second = channels.save(channel(ChannelType.PUBLIC));
            readStatuses.save(new ReadStatus(first, owner, Instant.EPOCH));
            readStatuses.save(new ReadStatus(first, manager, Instant.EPOCH));
            readStatuses.save(new ReadStatus(second, owner, Instant.EPOCH));
        });
        authenticate(owner);
    }

    @AfterEach
    void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        jwtRegistry.invalidateJwtInformationByUserId(owner.getId());
        jwtRegistry.invalidateJwtInformationByUserId(manager.getId());
        transaction.executeWithoutResult(status -> {
            notifications.deleteAllInBatch();
            messages.deleteAllInBatch();
            readStatuses.deleteAllInBatch();
            channels.deleteAllInBatch();
            users.deleteAllInBatch();
            binaryContents.deleteAllInBatch();
        });
        clearCaches();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 18})
    void channelList_batchesMissesRegardlessOfChannelAndParticipantCount(int additionalCount) {
        transaction.executeWithoutResult(status -> {
            for (int i = 0; i < additionalCount; i++) {
                BinaryContent profile = binaryContents.save(BinaryContent.createProcessing("profile.png", "image/png", 1024L));
                User participant = user();
                participant.updateInfo(new UserUpdateCommand(null, null, null), profile);
                participant = users.save(participant);
                Channel channel = channels.save(channel(ChannelType.PUBLIC));
                readStatuses.save(new ReadStatus(channel, participant, Instant.EPOCH));
            }
        });
        statistics.clear();
        var cold = channelService.findAllByUserId(owner.getId());
        long coldSql = statistics.getPrepareStatementCount();
        assertThat(cold).hasSize(2 + additionalCount);
        statistics.clear();
        var warm = channelService.findAllByUserId(owner.getId());
        long warmSql = statistics.getPrepareStatementCount();
        assertThat(warm).containsExactlyInAnyOrderElementsOf(cold);
        // 사용자 존재 확인 + 채널 ID + 참여 관계 ID 쿼리는 캐시 적중 시에도 실행한다.
        assertThat(coldSql).isEqualTo(5);
        assertThat(warmSql).isEqualTo(3);

        statistics.clear();
        assertThat(channelReader.findChannelSummary(first.getId())).contains(cachedChannel(first));
        assertThat(userReader.findUserDto(owner.getId())).contains(cachedUser(owner));
        assertThat(statistics.getPrepareStatementCount()).isZero();

        channelCache().evict(first.getId());
        userCache().evict(manager.getId());
        statistics.clear();
        assertThat(channelService.findAllByUserId(owner.getId())).containsExactlyInAnyOrderElementsOf(cold);
        long mixedSql = statistics.getPrepareStatementCount();
        assertThat(mixedSql).isEqualTo(5);
        System.out.printf("채널 %d개 목록 SQL 비교: 전체 누락=%d, 전체 적중=%d, 채널·사용자 각 1개 누락=%d%n",
                cold.size(), coldSql, warmSql, mixedSql);
    }

    @Test
    void userList_sharesUserEntriesAndReadsIdsOnEveryRequest() {
        statistics.clear();
        assertThat(userService.findAll()).extracting(UserDto::id).contains(owner.getId(), manager.getId());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        UserDto cachedOwner = cachedUser(owner);
        channelService.findAllByUserId(owner.getId());
        statistics.clear();
        assertThat(userService.findAll()).extracting(UserDto::id).contains(owner.getId(), manager.getId());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        assertThat(cachedUser(owner)).isSameAs(cachedOwner);

        User added = users.save(user());
        assertThat(userService.findAll()).extracting(UserDto::id).contains(added.getId());
        authenticate(added);
        userService.delete(added.getId());
        assertThat(userService.findAll()).extracting(UserDto::id).doesNotContain(added.getId());
        assertThat(userCache().get(added.getId())).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void channelUpdate_invalidatesOnlyItsEntryAfterCommit(boolean rollback) {
        channelService.findAllByUserId(owner.getId());
        ChannelSummary original = cachedChannel(first);
        ChannelSummary unaffected = cachedChannel(second);
        UserDto participant = cachedUser(owner);
        authenticate(manager);
        transaction.executeWithoutResult(status -> {
            channelService.update(first.getId(), new ChannelUpdateCommand("updated", "description"));
            assertThat(cachedChannel(first)).isSameAs(original);
            if (rollback) status.setRollbackOnly();
        });
        assertThat(cachedChannel(second)).isSameAs(unaffected);
        assertThat(cachedUser(owner)).isSameAs(participant);
        if (rollback) assertThat(cachedChannel(first)).isSameAs(original);
        else assertThat(channelCache().get(first.getId())).isNull();
        assertThat(channelService.findById(first.getId()).name()).isEqualTo(rollback ? original.name() : "updated");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void userUpdate_invalidatesOnlyItsEntryAfterCommit(boolean rollback) {
        channelService.findAllByUserId(owner.getId());
        UserDto original = cachedUser(owner);
        UserDto unaffected = cachedUser(manager);
        ChannelSummary channel = cachedChannel(first);
        transaction.executeWithoutResult(status -> {
            userService.update(owner.getId(), new UserUpdateCommand("updated-user", null, owner.getEmail()), null);
            assertThat(cachedUser(owner)).isSameAs(original);
            if (rollback) status.setRollbackOnly();
        });
        assertThat(cachedUser(manager)).isSameAs(unaffected);
        assertThat(cachedChannel(first)).isSameAs(channel);
        if (rollback) assertThat(cachedUser(owner)).isSameAs(original);
        else assertThat(userCache().get(owner.getId())).isNull();
        assertThat(channelService.findById(first.getId()).participants()).filteredOn(dto -> dto.id().equals(owner.getId()))
                .singleElement().extracting(UserDto::username).isEqualTo(rollback ? original.username() : "updated-user");
    }

    @Test
    void messageCreateAndDelete_refreshLatestTimeAndKeepOtherEntries() {
        Message old = messages.save(new Message(owner, first, new MessageCreateCommand("old", owner.getId(), first.getId())));
        Instant oldTime = messages.findById(old.getId()).orElseThrow().getCreatedAt();
        channelService.findAllByUserId(owner.getId());
        ChannelSummary unaffected = cachedChannel(second);
        UserDto participant = cachedUser(owner);
        UUID latest = messageService.save(new MessageCreateCommand("new", owner.getId(), first.getId()), List.of()).id();
        assertThat(channelCache().get(first.getId())).isNull();
        assertThat(channelService.findById(first.getId()).lastMessageAt().toInstant())
                .isEqualTo(messages.findById(latest).orElseThrow().getCreatedAt());

        messageService.delete(latest);
        assertThat(channelCache().get(first.getId())).isNull();
        assertThat(channelService.findById(first.getId()).lastMessageAt().toInstant()).isEqualTo(oldTime);
        messageService.delete(old.getId());
        assertThat(channelService.findById(first.getId()).lastMessageAt()).isNull();
        assertThat(cachedChannel(second)).isSameAs(unaffected);
        assertThat(cachedUser(owner)).isSameAs(participant);
    }

    @Test
    void channelDeletion_removesItsEntryAndTheLiveIdList() {
        channelService.findAllByUserId(owner.getId());
        ChannelSummary unaffected = cachedChannel(second);
        authenticate(manager);
        channelService.delete(first.getId());
        assertThat(channelCache().get(first.getId())).isNull();
        assertThat(channelService.findAllByUserId(manager.getId())).extracting(dto -> dto.id()).containsExactly(second.getId());
        assertThat(cachedChannel(second)).isSameAs(unaffected);
        assertThatThrownBy(() -> channelService.findById(first.getId())).isInstanceOf(ChannelNotFoundException.class);
    }

    @Test
    void membershipAndVisibilityAreReadLiveEvenWhenChannelIsCached() {
        Channel privateChannel = channels.save(channel(ChannelType.PRIVATE));
        authenticate(manager);
        channelService.findAllByUserId(manager.getId()); // 관리자의 조회로 비공개 채널 캐시도 채운다.
        ChannelSummary cached = cachedChannel(privateChannel);
        authenticate(owner);
        assertThat(channelService.findAllByUserId(owner.getId())).extracting(dto -> dto.id()).doesNotContain(privateChannel.getId());
        ReadStatus membership = readStatuses.save(new ReadStatus(privateChannel, owner, Instant.EPOCH));
        assertThat(channelService.findAllByUserId(owner.getId())).filteredOn(dto -> dto.id().equals(privateChannel.getId()))
                .singleElement().satisfies(dto -> assertThat(dto.participants()).extracting(UserDto::id).containsExactly(owner.getId()));
        readStatuses.deleteById(membership.getId());
        assertThat(channelService.findAllByUserId(owner.getId())).extracting(dto -> dto.id()).doesNotContain(privateChannel.getId());
        assertThat(cachedChannel(privateChannel)).isSameAs(cached);
    }

    @Test
    void readerProxies_cacheSingleResultsAndDoNotCacheMissingObjects() {
        statistics.clear();
        var summary = channelReader.findChannelSummary(first.getId()).orElseThrow();
        var user = userReader.findUserDto(owner.getId()).orElseThrow();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);

        statistics.clear();
        assertThat(channelReader.findChannelSummary(first.getId())).contains(summary);
        assertThat(userReader.findUserDto(owner.getId())).contains(user);
        assertThat(channelReader.findChannelSummariesByIds(List.of(first.getId()))).containsEntry(first.getId(), summary);
        assertThat(userReader.findUserDtosByIds(List.of(owner.getId()))).containsEntry(owner.getId(), user);
        assertThat(statistics.getPrepareStatementCount()).isZero();
        // Optional 자체가 아니라 반환된 DTO가 ID별로 저장된다.
        assertThat(cachedChannel(first)).isEqualTo(summary);
        assertThat(cachedUser(owner)).isEqualTo(user);

        UUID missing = UUID.randomUUID();
        assertThat(channelReader.findChannelSummary(missing)).isEmpty();
        assertThat(userReader.findUserDto(missing)).isEmpty();
        assertThat(channelReader.findChannelSummary(missing)).isEmpty();
        assertThat(userReader.findUserDto(missing)).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
        assertThat(channelCache().get(missing)).isNull();
        assertThat(userCache().get(missing)).isNull();
        assertThatThrownBy(() -> channelService.findById(missing)).isInstanceOf(ChannelNotFoundException.class);
        assertThatThrownBy(() -> userService.findById(missing)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void loginAndLogout_refreshOnlyThatUsersOnlineStateWithoutTransaction() throws Exception {
        channelService.findAllByUserId(owner.getId());
        UserDto unaffected = cachedUser(manager);
        ChannelSummary channel = cachedChannel(first);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        loginHandler.onAuthenticationSuccess(request, response, SecurityContextHolder.getContext().getAuthentication());
        assertThat(userCache().get(owner.getId())).isNull();
        assertThat(userService.findById(owner.getId()).online()).isTrue();
        assertThat(channelService.findById(first.getId()).participants()).filteredOn(dto -> dto.id().equals(owner.getId()))
                .singleElement().extracting(UserDto::online).isEqualTo(true);

        request.setCookies(MockCookie.parse(response.getHeader(HttpHeaders.SET_COOKIE)));
        logoutHandler.logout(request, response, null);
        assertThat(userCache().get(owner.getId())).isNull();
        assertThat(userService.findById(owner.getId()).online()).isFalse();
        assertThat(cachedUser(manager)).isSameAs(unaffected);
        assertThat(cachedChannel(first)).isSameAs(channel);
    }

    @Test
    void expiredTokenCleanup_refreshesOnlyAffectedUsers() {
        jwtRegistry.registerJwtInformation(new JwtInformation(principal(owner).getUserDto(), "access", "expired-refresh"));
        channelService.findAllByUserId(owner.getId());
        UserDto unaffected = cachedUser(manager);
        assertThat(cachedUser(owner).online()).isTrue();
        jwtRegistry.clearExpiredJwtInformation();
        assertThat(userCache().get(owner.getId())).isNull();
        assertThat(userService.findById(owner.getId()).online()).isFalse();
        assertThat(cachedUser(manager)).isSameAs(unaffected);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void roleUpdate_refreshesUserAfterJwtInvalidationEvenWhenRoleIsUnchanged(boolean changeRole) {
        jwtRegistry.registerJwtInformation(new JwtInformation(principal(owner).getUserDto(), "access", "refresh"));
        channelService.findAllByUserId(owner.getId());
        ChannelSummary channel = cachedChannel(first);
        Role role = changeRole ? Role.CHANNEL_MANAGER : Role.USER;
        roles.updateRole(owner.getId(), new UserRoleUpdateCommand(role));
        assertThat(userCache().get(owner.getId())).isNull();
        UserDto updated = userService.findById(owner.getId());
        assertThat(updated.role()).isEqualTo(role);
        assertThat(updated.online()).isFalse();
        assertThat(cachedChannel(first)).isSameAs(channel);
    }

    @Test
    void sharedUserCache_preservesRequestTimeZone() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Time-Zone", "Asia/Seoul");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        UserDto firstResponse = userService.findById(owner.getId());
        UserDto cached = cachedUser(owner);
        request.removeHeader("Time-Zone");
        request.addHeader("Time-Zone", "UTC");
        UserDto nextResponse = userService.findById(owner.getId());
        assertThat(firstResponse.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(nextResponse.createdAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(nextResponse.createdAt().toInstant()).isEqualTo(firstResponse.createdAt().toInstant());
        assertThat(cachedUser(owner)).isSameAs(cached);
    }

    @Test
    void notificationCache_remainsScopedToReceiver() {
        authenticate(manager);
        notificationService.findAllForCurrentUser();
        var other = cacheManager.getCache(CacheConfig.USERS_BY_NOTIFICATIONS).get(manager.getId()).get();
        authenticate(owner);
        assertThat(notificationService.findAllForCurrentUser()).isEmpty();
        notificationService.createAll(new NotificationCreateCommand(List.of(owner.getId()), "title", "body"));
        var created = notificationService.findAllForCurrentUser();
        assertThat(created).hasSize(1);
        notificationService.delete(created.get(0).id());
        assertThat(notificationService.findAllForCurrentUser()).isEmpty();
        assertThat(cacheManager.getCache(CacheConfig.USERS_BY_NOTIFICATIONS).get(manager.getId()).get()).isSameAs(other);
    }

    @Test
    void actuator_exposesNamedCachesAndHitStatisticsToAdmin() throws Exception {
        userReader.findUserDto(owner.getId());
        userReader.findUserDto(owner.getId());
        String adminToken = bearer(manager);
        mvc.perform(get("/actuator/caches").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheManagers.cacheManager.caches.channels").exists())
                .andExpect(jsonPath("$.cacheManagers.cacheManager.caches.users").exists())
                .andExpect(jsonPath("$.cacheManagers.cacheManager.caches.usersByNotifications").exists());
        mvc.perform(get("/actuator/metrics/cache.gets").param("tag", "cache:users", "result:hit")
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.measurements[0].value").value(org.hamcrest.Matchers.greaterThan(0.0)));
        mvc.perform(get("/actuator/caches").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isForbidden());
    }

    private void clearCaches() { cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear()); }
    private Cache channelCache() { return cacheManager.getCache(CacheConfig.CHANNELS); }
    private Cache userCache() { return cacheManager.getCache(CacheConfig.USERS); }
    private ChannelSummary cachedChannel(Channel channel) { return channelCache().get(channel.getId(), ChannelSummary.class); }
    private UserDto cachedUser(User user) { return userCache().get(user.getId(), UserDto.class); }
    private void authenticate(User user) {
        DiscodeitUserDetails principal = principal(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }
    private String bearer(User user) {
        String access = tokens.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = tokens.generateRefreshToken(user.getId(), user.getUsername());
        jwtRegistry.registerJwtInformation(new JwtInformation(principal(user).getUserDto(), access, refresh));
        return "Bearer " + access;
    }
}
