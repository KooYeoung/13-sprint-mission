package com.sprint.mission.discodeit.dto.request.channel;

import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import jakarta.validation.constraints.NotBlank;

public record ChannelUpdateRequest(
        @NotBlank(message = ValidationMessage.CHANNEL_NAME_MESSAGE)
        String newName,
        String newDescription
) {
    public ChannelUpdateCommand toCommand() {
        return new ChannelUpdateCommand(newName, newDescription);
    }
}