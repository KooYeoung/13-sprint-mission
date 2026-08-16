package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class InvalidChannelTypeException extends ChannelException {
    public InvalidChannelTypeException(ErrorCode errorCode) {
        super(errorCode);
    }

}
