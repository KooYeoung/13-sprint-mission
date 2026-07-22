package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public class ChannelTypeInvalidException extends ChannelException {
    public ChannelTypeInvalidException(String type) {
        super(ErrorCode.CHANNEL_TYPE_INVALID, Map.of("type", String.valueOf(type)));
    }
}
