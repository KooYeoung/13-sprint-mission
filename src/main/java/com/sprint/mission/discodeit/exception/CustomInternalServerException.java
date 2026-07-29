package com.sprint.mission.discodeit.exception;

public class CustomInternalServerException extends DiscodeitException {

    public CustomInternalServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CustomInternalServerException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
