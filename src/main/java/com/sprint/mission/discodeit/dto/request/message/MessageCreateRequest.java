package com.sprint.mission.discodeit.dto.request.message;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MessageCreateRequest(
        @NotBlank(message = ValidationMessage.MESSAGE_CONTENT)
        String content,
        @NotNull(message = ValidationMessage.CHANNEL_ID_MESSAGE)
        UUID channelId,
        @NotNull(message = ValidationMessage.USER_ID_MESSAGE)
        UUID authorId
) {
    public MessageCreateCommand toCommand() {
        return new MessageCreateCommand(content, authorId, channelId);
    }
}