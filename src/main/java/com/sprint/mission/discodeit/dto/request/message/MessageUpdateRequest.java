package com.sprint.mission.discodeit.dto.request.message;

import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.NotBlank;

public record MessageUpdateRequest(
        @NotBlank(message = ValidationMessage.MESSAGE_CONTENT)
        String newContent
) {
    public MessageUpdateCommand toCommand() {
        return new MessageUpdateCommand(newContent);
    }
}