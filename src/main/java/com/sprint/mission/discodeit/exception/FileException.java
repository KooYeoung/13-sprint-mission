package com.sprint.mission.discodeit.exception;

import java.util.Map;

public class FileException extends DiscodeitException {

    public FileException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }

    public FileException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public FileException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        super(errorCode, details, cause);
    }
}
