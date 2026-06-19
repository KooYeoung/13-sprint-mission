package com.sprint.mission.discodeit.dto.request.channel;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePrivateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePublicCommand;
import com.sprint.mission.discodeit.entity.ChannelType;

import java.util.List;
import java.util.UUID;

public record PrivateChannelCreateRequest(
        List<UUID> participantIds
) {
    public ChannelCreatePrivateCommand toCommand() {
        return new ChannelCreatePrivateCommand(participantIds, ChannelType.PRIVATE);
    }
}