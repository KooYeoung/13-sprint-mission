package com.sprint.mission.discodeit.dto.command.channel;

import com.sprint.mission.discodeit.entity.ChannelType;

import java.util.List;
import java.util.UUID;

public record ChannelCreatePrivateCommand(
        List<UUID> participantIds,
        ChannelType channelType
) implements ChannelCreateCommand {

    @Override
    public String channelName() {
        return "";
    }

    @Override
    public String channelDescription() {
        return "";
    }

    @Override
    public boolean isPrivate() {
        return ChannelType.PRIVATE.equals(channelType);
    }
}
