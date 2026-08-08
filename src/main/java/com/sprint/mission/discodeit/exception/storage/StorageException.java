package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.DiscodeitException;
import com.sprint.mission.discodeit.exception.ErrorCode;

import java.nio.file.Path;
import java.util.Map;

public abstract class StorageException extends DiscodeitException {

    private final Path path;

    protected StorageException(ErrorCode errorCode, Map<String, Object> details, Path path) {
        super(errorCode, details);
        this.path = path;
    }

    protected StorageException(ErrorCode errorCode, Map<String, Object> details, Path path, Throwable cause) {
        super(errorCode, details, cause);
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

}
