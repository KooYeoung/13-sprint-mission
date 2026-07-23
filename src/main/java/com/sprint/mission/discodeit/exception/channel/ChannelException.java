package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.DiscodeitException;
import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public abstract class ChannelException extends DiscodeitException {

    protected ChannelException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected ChannelException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }

    protected ChannelException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        super(errorCode, details, cause);
    }
}
