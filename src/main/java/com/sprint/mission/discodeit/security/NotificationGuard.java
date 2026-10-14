package com.sprint.mission.discodeit.security;

import com.sprint.mission.discodeit.service.basic.NotificationReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationGuard {
    private final NotificationReader notificationReader;

    public boolean isOwner(UUID notificationId, UUID userId) {
        return notificationReader.getNotification(notificationId)
                .getReceiverId().equals(userId);
    }
}
