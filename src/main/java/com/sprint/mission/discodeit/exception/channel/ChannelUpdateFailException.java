package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.CustomBadRequestException;

public class ChannelUpdateFailException extends CustomBadRequestException {
    public ChannelUpdateFailException(String message) {
        super(message);
    }
}
