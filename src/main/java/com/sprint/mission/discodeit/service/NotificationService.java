package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.dto.response.NotificationDto;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void createAll(NotificationCreateCommand command);

    // 알림 확인
    void delete(UUID notificationId);

    List<NotificationDto> findAllForCurrentUser();
}
