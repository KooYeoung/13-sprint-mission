package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.basic.ReadStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCreationEventListenerTest {
    @Mock ReadStatusService readStatusService;
    @Mock NotificationService notificationService;
    @InjectMocks NotificationCreationEventListener listener;

    @Test
    void message_createsNotificationsForRecipientsReturnedByReadStatusService() {
        MessageCreatedEvent event = new MessageCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), "general", "sender", "hello");
        List<UUID> recipients = List.of(UUID.randomUUID(), UUID.randomUUID());
        given(readStatusService.findNotificationRecipientIds(event.channelId(), event.authorId())).willReturn(recipients);

        listener.handleMessageCreated(event);

        verify(notificationService).createAll(new NotificationCreateCommand(recipients, "sender (#general)", "hello"));
    }

    @Test
    void message_doesNotCreateNotificationsWhenNobodySubscribed() {
        MessageCreatedEvent event = new MessageCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), "general", "sender", "hello");
        given(readStatusService.findNotificationRecipientIds(event.channelId(), event.authorId())).willReturn(List.of());
        listener.handleMessageCreated(event);
        verifyNoInteractions(notificationService);
    }

    @Test
    void role_createsNotificationOnlyForAffectedUser() {
        UUID userId = UUID.randomUUID();
        listener.handleRoleUpdated(new RoleUpdatedEvent(userId, Role.USER, Role.CHANNEL_MANAGER));
        verify(notificationService).createAll(new NotificationCreateCommand(List.of(userId), "권한이 변경되었습니다.", "USER -> CHANNEL_MANAGER"));
        verifyNoInteractions(readStatusService);
    }

}
