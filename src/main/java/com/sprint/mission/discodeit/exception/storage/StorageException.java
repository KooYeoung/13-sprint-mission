package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.DiscodeitException;
import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public abstract class StorageException extends DiscodeitException {

    protected StorageException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode, details);
    }

    protected StorageException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    protected StorageException(ErrorCode errorCode, Map<String, Object> details, Throwable cause) {
        super(errorCode, details, cause);
    }
}
