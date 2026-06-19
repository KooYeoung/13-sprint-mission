package com.sprint.mission.discodeit.dto.request.message;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;

import java.util.UUID;

public record MessageCreateRequest(
        String content,
        UUID channelId,
        UUID authorId
) {
    public MessageCreateCommand toCommand() {
        return new MessageCreateCommand(content, authorId, channelId);
    }
}