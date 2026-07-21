package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.ChannelException;
import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class ChannelUpdateFailException extends ChannelException {
    public ChannelUpdateFailException(UUID channelId) {
        super(ErrorCode.PRIVATE_CHANNEL_UPDATE_NOT_ALLOWED, Map.of("channelId", channelId));
    }

}
