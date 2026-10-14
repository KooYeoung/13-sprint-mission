package com.sprint.mission.discodeit.integration;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserRoleUpdateCommand;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.repository.*;
import com.sprint.mission.discodeit.security.*;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.basic.ReadStatusService;
import com.sprint.mission.discodeit.service.basic.UserRoleManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.sprint.mission.discodeit.support.AsyncTestSupport.awaitIdle;
import static org.awaitility.Awaitility.await;
import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "discodeit.storage.type=local"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("알림 API 및 실제 커밋·롤백 통합 테스트")
class NotificationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ChannelRepository channels;
    @Autowired ReadStatusRepository readStatuses;
    @Autowired MessageRepository messages;
    @Autowired NotificationRepository notifications;
    @Autowired MessageService messageService;
    @Autowired NotificationService notificationService;
    @Autowired ReadStatusService readStatusService;
    @Autowired UserRoleManager roles;
    @Autowired JwtTokenProvider tokens;
    @Autowired JwtRegistry jwtRegistry;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ThreadPoolTaskExecutor notificationExecutor;

    private TransactionTemplate transaction;
    private User sender;
    private User receiver;
    private User disabled;
    private User outsider;
    private Channel channel;
    private ReadStatus receiverStatus;

    @BeforeEach
    void commitFixtures() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            sender = users.save(user());
            receiver = users.save(user());
            disabled = users.save(user());
            outsider = users.save(user());
            channel = channels.save(channel(ChannelType.PUBLIC));
            Channel otherChannel = channels.save(channel(ChannelType.PUBLIC));
            receiverStatus = subscribed(channel, receiver, true);
            subscribed(channel, sender, true);
            subscribed(channel, disabled, false);
            subscribed(otherChannel, outsider, true);
        });
    }

    @AfterEach
    void cleanCommittedData() {
        awaitIdle(notificationExecutor);
        SecurityContextHolder.clearContext();
        List<UUID> userIds = List.of(sender.getId(), receiver.getId(), disabled.getId(), outsider.getId());
        userIds.forEach(jwtRegistry::invalidateJwtInformationByUserId);
        transaction.executeWithoutResult(status -> {
            notifications.deleteAllInBatch();
            messages.deleteAllInBatch();
            readStatuses.deleteAllInBatch();
            channels.deleteAllInBatch();
            users.deleteAllById(userIds);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void messageNotifications_followMainTransaction(boolean rollback) {
        authenticate(sender);
        UUID messageId = transaction.execute(status -> {
            UUID id = messageService.save(new MessageCreateCommand("hello", sender.getId(), channel.getId()), List.of()).id();
            assertThat(notifications.count()).isZero();
            if (rollback) status.setRollbackOnly();
            return id;
        });

        // 새 트랜잭션에서 재조회하므로 커밋 후 별도 저장까지 검증한다.
        if (!rollback) await().untilAsserted(() -> assertThat(notifications.count()).isEqualTo(1));
        transaction.executeWithoutResult(status -> {
            assertThat(messages.existsById(messageId)).isEqualTo(!rollback);
            if (rollback) {
                assertThat(notifications.count()).isZero();
            } else {
                assertThat(notifications.findAll()).singleElement().satisfies(notification -> {
                    assertThat(notification.getReceiverId()).isEqualTo(receiver.getId());
                    assertThat(notification.getTitle()).isEqualTo(sender.getUsername() + " (#test-channel)");
                    assertThat(notification.getContent()).isEqualTo("hello");
                });
            }
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void roleNotifications_followMainTransaction(boolean rollback) {
        transaction.executeWithoutResult(status -> {
            roles.updateRole(receiver.getId(), new UserRoleUpdateCommand(Role.CHANNEL_MANAGER));
            assertThat(notifications.count()).isZero();
            if (rollback) status.setRollbackOnly();
        });
        if (!rollback) await().untilAsserted(() -> assertThat(notifications.count()).isEqualTo(1));
        transaction.executeWithoutResult(status -> {
            assertThat(users.findById(receiver.getId()).orElseThrow().getRole())
                    .isEqualTo(rollback ? Role.USER : Role.CHANNEL_MANAGER);
            if (rollback) {
                assertThat(notifications.count()).isZero();
            } else {
                assertThat(notifications.findAll()).singleElement().satisfies(notification -> {
                    assertThat(notification.getReceiverId()).isEqualTo(receiver.getId());
                    assertThat(notification.getTitle()).isEqualTo("권한이 변경되었습니다.");
                    assertThat(notification.getContent()).isEqualTo("USER -> CHANNEL_MANAGER");
                });
            }
        });
    }

    @Test
    void unchangedRole_doesNotCreateNotificationAfterCommit() {
        roles.updateRole(receiver.getId(), new UserRoleUpdateCommand(Role.USER));
        assertThat(notifications.count()).isZero();
    }

    @Test
    void notificationCreation_commitsIndependentlyOfCallerRollback() {
        transaction.executeWithoutResult(status -> {
            notificationService.createAll(command(receiver));
            status.setRollbackOnly();
        });
        assertThat(notifications.findAllByReceiverId(receiver.getId())).hasSize(1);
    }

    @Test
    void missingParticipant_rollsBackPartiallyInsertedReadStatuses() {
        AtomicReference<UUID> channelId = new AtomicReference<>();
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Channel created = channels.save(channel(ChannelType.PRIVATE));
            channelId.set(created.getId());
            readStatusService.saveAll(created, List.of(sender.getId(), UUID.randomUUID()), Instant.now());
        })).isInstanceOf(UserNotFoundException.class);
        assertThat(channels.existsById(channelId.get())).isFalse();
        assertThat(readStatuses.findByChannelId(channelId.get())).isEmpty();
    }

    @Test
    void getNotifications_returnsOnlyAuthenticatedReceiversData() throws Exception {
        UUID ownId = createNotification(receiver);
        createNotification(sender);
        mvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, bearer(receiver)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ownId.toString()))
                .andExpect(jsonPath("$[0].receiverId").value(receiver.getId().toString()))
                .andExpect(jsonPath("$[0].title").value("title"))
                .andExpect(jsonPath("$[0].content").value("content"))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
    }

    @Test
    void getNotifications_returnsEmptyListWhenNoNotificationsExist() throws Exception {
        mvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, bearer(receiver)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void notificationEndpoints_requireAuthentication() throws Exception {
        UUID notificationId = createNotification(receiver);
        mvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(delete("/api/notifications/{id}", notificationId).with(csrf()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        assertThat(notifications.existsById(notificationId)).isTrue();
    }

    @Test
    void deleteNotification_removesOwnNotification() throws Exception {
        UUID notificationId = createNotification(receiver);
        mvc.perform(delete("/api/notifications/{id}", notificationId).with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receiver)))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(notifications.existsById(notificationId)).isFalse();
    }

    @Test
    void deleteNotification_rejectsOtherUsersNotification() throws Exception {
        UUID notificationId = createNotification(receiver);
        mvc.perform(delete("/api/notifications/{id}", notificationId).with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(sender)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        assertThat(notifications.existsById(notificationId)).isTrue();
    }

    @Test
    void deleteNotification_returns404ForMissingId() throws Exception {
        mvc.perform(delete("/api/notifications/{id}", UUID.randomUUID()).with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receiver)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void readStatusUpdate_changesOwnersPreference() throws Exception {
        mvc.perform(patch("/api/readStatuses/{id}", receiverStatus.getId()).with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receiver)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastReadAt\":\"2026-10-02T00:00:00Z\",\"newNotificationEnabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notificationEnabled").value(false));
        ReadStatus stored = readStatuses.findById(receiverStatus.getId()).orElseThrow();
        assertThat(stored.getNotificationEnabled()).isFalse();
        assertThat(stored.getLastReadAt()).isEqualTo(Instant.parse("2026-10-02T00:00:00Z"));
    }

    @Test
    void readStatusUpdate_rejectsOtherUsersPreferenceChange() throws Exception {
        mvc.perform(patch("/api/readStatuses/{id}", receiverStatus.getId()).with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(sender)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastReadAt\":\"2026-10-02T00:00:00Z\",\"newNotificationEnabled\":false}"))
                .andExpect(status().isForbidden());
        assertThat(readStatuses.findById(receiverStatus.getId()).orElseThrow().getNotificationEnabled()).isTrue();
    }

    @Test
    void readStatusUpdate_returns404ForMissingId() throws Exception {
        mvc.perform(patch("/api/readStatuses/{id}", UUID.randomUUID()).with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receiver)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLastReadAt\":\"2026-10-02T00:00:00Z\",\"newNotificationEnabled\":false}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("READ_STATUS_NOT_FOUND"));
    }

    private ReadStatus subscribed(Channel channel, User user, boolean enabled) {
        ReadStatus readStatus = new ReadStatus(channel, user, Instant.EPOCH);
        readStatus.updateInfo(new ReadStatusUpdateCommand(Instant.EPOCH, enabled));
        return readStatuses.save(readStatus);
    }

    private NotificationCreateCommand command(User receiver) {
        return new NotificationCreateCommand(List.of(receiver.getId()), "title", "content");
    }

    private UUID createNotification(User receiver) {
        notificationService.createAll(command(receiver));
        return notifications.findAllByReceiverId(receiver.getId()).get(0).getId();
    }

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
