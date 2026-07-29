package com.sprint.mission.discodeit.exception;

import java.util.Map;

public class InvalidRequestException extends DiscodeitException {
    public InvalidRequestException(Map<String, Object> details) {
        super(ErrorCode.INVALID_REQUEST, details);
    }
}
