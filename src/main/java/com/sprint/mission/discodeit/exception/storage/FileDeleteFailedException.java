package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class FileDeleteFailedException extends StorageException {
    public FileDeleteFailedException(Path path, Throwable cause) {
        super(
                ErrorCode.FILE_DELETE_FAILED,
                Map.of(),
                path,
                cause
        );
    }

    public FileDeleteFailedException(UUID fileId, String message, Throwable cause) {
        super(
                ErrorCode.FILE_DELETE_FAILED,
                message,
                Map.of("fileId", fileId),
                null,
                cause
        );
    }
}
