package com.sprint.mission.discodeit.dto.request.channel;

import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;

public record ChannelUpdateRequest(
        String newName,
        String newDescription
) {
    public ChannelUpdateCommand toCommand() {
        return new ChannelUpdateCommand(newName, newDescription);
    }
}