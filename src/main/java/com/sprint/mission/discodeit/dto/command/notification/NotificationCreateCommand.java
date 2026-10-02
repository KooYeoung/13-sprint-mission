package com.sprint.mission.discodeit.dto.command.notification;

import com.sprint.mission.discodeit.events.NotificationContent;

import java.util.List;
import java.util.UUID;

public record NotificationCreateCommand(
        List<UUID> receiverIds,
        String title,
        String content
) {
    public NotificationCreateCommand(UUID receiverId, NotificationContent notificationContent) {
        this(List.of(receiverId), notificationContent);
    }

    public NotificationCreateCommand(List<UUID> receiverIds, NotificationContent notificationContent) {
        this(receiverIds,
                truncateNotificationText(notificationContent.title()),
                truncateNotificationText(notificationContent.content()));
    }

    private static String truncateNotificationText(String value) {
        int maxCodePoints = 255;
        String suffix = "...";

        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount <= maxCodePoints) {
            return value;
        }

        int endIndex = value.offsetByCodePoints(0, maxCodePoints - suffix.length());

        return value.substring(0, endIndex) + suffix;
    }

}
