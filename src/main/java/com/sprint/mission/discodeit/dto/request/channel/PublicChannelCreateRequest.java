package com.sprint.mission.discodeit.dto.request.channel;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePublicCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import com.sprint.mission.discodeit.entity.ChannelType;
import jakarta.validation.constraints.NotBlank;

public record PublicChannelCreateRequest(
        @NotBlank(message = ValidationMessage.CHANNEL_NAME_MESSAGE)
        String name,
        String description
) {
    public ChannelCreatePublicCommand toCommand() {
        return new ChannelCreatePublicCommand(name, description, ChannelType.PUBLIC);
    }
}