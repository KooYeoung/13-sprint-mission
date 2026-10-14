package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.exception.notification.NotificationNotFoundException;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationReader {

    private final NotificationRepository notificationRepository;

    public Notification getNotification(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(()-> new NotificationNotFoundException(notificationId));
    }
}
