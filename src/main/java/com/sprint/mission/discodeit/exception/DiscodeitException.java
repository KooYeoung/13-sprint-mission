package com.sprint.mission.discodeit.exception;

import java.util.Map;

public abstract class DiscodeitException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    protected DiscodeitException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), Map.of(), null);
    }

    protected DiscodeitException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, errorCode.getMessage(), Map.of(), cause);
    }

    protected DiscodeitException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode, errorCode.getMessage(), details, null);
    }

    protected DiscodeitException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        this(errorCode, errorCode.getMessage(), details, cause);
    }

    protected DiscodeitException(ErrorCode errorCode, String message, Map<String, Object> details, Throwable cause) {
        super(message == null || message.isBlank() ? errorCode.getMessage() : message, cause);
        this.errorCode = errorCode;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
