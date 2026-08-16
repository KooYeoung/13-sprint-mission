package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class FileSaveFailedException extends StorageException {
    public FileSaveFailedException(Path path, Throwable cause) {
        super(
                ErrorCode.FILE_SAVE_FAILED,
                Map.of(),
                path,
                cause
        );
    }

    public FileSaveFailedException(UUID fileId, String message, Throwable cause) {
        super(
                ErrorCode.FILE_SAVE_FAILED,
                message,
                Map.of("fileId", fileId),
                null,
                cause
        );
    }
}
