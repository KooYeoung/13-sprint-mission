package com.sprint.mission.discodeit.dto.request.channel;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePublicCommand;
import com.sprint.mission.discodeit.entity.ChannelType;

public record PublicChannelCreateRequest(
        String name,
        String description
) {
    public ChannelCreatePublicCommand toCommand() {
        return new ChannelCreatePublicCommand(name, description, ChannelType.PUBLIC);
    }
}