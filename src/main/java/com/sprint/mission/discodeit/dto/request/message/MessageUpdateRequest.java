package com.sprint.mission.discodeit.dto.request.message;

import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;

public record MessageUpdateRequest(
        String newContent
) {
    public MessageUpdateCommand toCommand() {
        return new MessageUpdateCommand(newContent);
    }
}