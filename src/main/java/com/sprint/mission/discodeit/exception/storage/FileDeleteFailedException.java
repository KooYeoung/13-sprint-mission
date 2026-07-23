package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public class FileDeleteFailedException extends StorageException {
    public FileDeleteFailedException(Throwable cause) {
        super(
                ErrorCode.FILE_DELETE_FAILED,
                Map.of(),
                cause
        );
    }
}
