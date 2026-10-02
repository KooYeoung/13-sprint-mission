package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.basic.ReadStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationCreationEventListener {

    private final ReadStatusService readStatusService;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageCreated(MessageCreatedEvent event) {
        List<UUID> recipientIds = readStatusService.findNotificationRecipientIds(event.channelId(), event.authorId());
        if (recipientIds.isEmpty()) {
            return;
        }
        notificationService.createAll(new NotificationCreateCommand(recipientIds, event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoleUpdated(RoleUpdatedEvent event) {
        notificationService.createAll(new NotificationCreateCommand(event.userId(), event));
    }
}
