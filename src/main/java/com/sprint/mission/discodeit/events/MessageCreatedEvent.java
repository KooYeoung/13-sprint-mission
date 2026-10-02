package com.sprint.mission.discodeit.events;

import java.util.UUID;

public record MessageCreatedEvent(
        UUID channelId,
        UUID authorId,
        String channelName,
        String authorName,
        String message
) implements NotificationContent {

    public static MessageCreatedEvent of(UUID channelId, UUID authorId, String channelName, String authorName, String content) {
        return new MessageCreatedEvent(
                channelId,
                authorId,
                channelName,
                authorName,
                content
        );
    }

    @Override
    public String title() {
        return String.format("%s (#%s)", authorName, channelName);
    }

    @Override
    public String content() {
        return message;
    }

}
