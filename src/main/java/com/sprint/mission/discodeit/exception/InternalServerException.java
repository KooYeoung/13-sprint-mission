package com.sprint.mission.discodeit.exception;

public class InternalServerException extends DiscodeitException {
    public InternalServerException(Throwable cause) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, cause);
    }
}
