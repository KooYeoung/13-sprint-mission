package com.sprint.mission.discodeit.dto.command.channel;

import com.sprint.mission.discodeit.entity.ChannelType;

public record ChannelCreatePublicCommand(
        String channelName,
        String channelDescription,
        ChannelType channelType
) implements ChannelCreateCommand {

    @Override
    public boolean isPrivate() {
        return ChannelType.PRIVATE.equals(channelType);
    }
}
