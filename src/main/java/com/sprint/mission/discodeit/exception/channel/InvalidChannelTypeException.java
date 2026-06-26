package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.CustomBadRequestException;

public class InvalidChannelTypeException extends CustomBadRequestException {
    public InvalidChannelTypeException(String message) {
        super(message);
    }
}
