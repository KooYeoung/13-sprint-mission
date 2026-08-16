package com.sprint.mission.discodeit.dto.command.channel;

import com.sprint.mission.discodeit.entity.ChannelType;

public interface ChannelCreateCommand {
    String channelName();

    String channelDescription();

    ChannelType channelType();

    boolean isPrivate();
}
