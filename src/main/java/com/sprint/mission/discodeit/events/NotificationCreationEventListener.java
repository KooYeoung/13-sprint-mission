package com.sprint.mission.discodeit.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCreationEventListener {
    private final NotificationCreationProcessor processor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageCreated(MessageCreatedEvent event) {
        try {
            processor.handleMessageCreated(event);
        } catch (TaskRejectedException rejection) {
            log.error("실행기가 메시지 알림 작업을 거부했습니다. channelId={}, authorId={}",
                    event.channelId(), event.authorId(), rejection);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoleUpdated(RoleUpdatedEvent event) {
        try {
            processor.handleRoleUpdated(event);
        } catch (TaskRejectedException rejection) {
            log.error("실행기가 권한 변경 알림 작업을 거부했습니다. userId={}, previousRole={}, newRole={}",
                    event.userId(), event.previousRole(), event.updatedRole(), rejection);
        }
    }
}
