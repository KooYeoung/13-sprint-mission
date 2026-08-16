package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class ChannelTypeRequiredException extends ChannelException {
    public ChannelTypeRequiredException() {
        super(ErrorCode.CHANNEL_TYPE_REQUIRED);
    }
}
