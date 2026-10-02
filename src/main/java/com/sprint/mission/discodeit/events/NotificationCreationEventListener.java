package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.service.NotificationService;
import com.sprint.mission.discodeit.service.basic.ReadStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCreationEventListener {

    private final ReadStatusService readStatusService;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageCreated(MessageCreatedEvent event) {
        try {
            List<UUID> recipientIds = readStatusService.findNotificationRecipientIds(event.channelId(), event.authorId());
            if (recipientIds.isEmpty()) {
                log.debug("알림을 받을 사용자가 없어 생성을 생략합니다. channelId={}, authorId={}",
                        event.channelId(), event.authorId());
                return;
            }
            notificationService.createAll(new NotificationCreateCommand(recipientIds, event));
            log.info("메시지 알림 생성을 완료했습니다. channelId={}, authorId={}, recipientCount={}",
                    event.channelId(), event.authorId(), recipientIds.size());
        } catch (RuntimeException e) {
            log.error("메시지 알림 생성에 실패했습니다. channelId={}, authorId={}",
                    event.channelId(), event.authorId(), e);
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoleUpdated(RoleUpdatedEvent event) {
        try {
            notificationService.createAll(new NotificationCreateCommand(event.userId(), event));
            log.info("권한 변경 알림 생성을 완료했습니다. userId={}, previousRole={}, newRole={}",
                    event.userId(), event.previousRole(), event.updatedRole());
        } catch (RuntimeException e) {
            log.error("권한 변경 알림 생성에 실패했습니다. userId={}, previousRole={}, newRole={}",
                    event.userId(), event.previousRole(), event.updatedRole(), e);
            throw e;
        }
    }
}
