package com.sprint.mission.discodeit.dto.command.channel;

public record ChannelUpdateCommand(
        String channelName,
        String channelDescription
) {
}
