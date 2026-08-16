package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class FileReadFailedException extends StorageException {
    public FileReadFailedException(UUID fileId, Path path, Throwable cause) {
        super(ErrorCode.FILE_READ_FAILED, Map.of("fileId", fileId), path, cause);
    }

    public FileReadFailedException(UUID fileId, String message, Throwable cause) {
        super(ErrorCode.FILE_READ_FAILED, message, Map.of("fileId", fileId), null, cause);
    }

    public FileReadFailedException(String fileName, Throwable cause) {
        super(ErrorCode.FILE_READ_FAILED, Map.of("fileName", String.valueOf(fileName)), null, cause);
    }

    public FileReadFailedException(String fileName, String message, Throwable cause) {
        super(ErrorCode.FILE_READ_FAILED, message, Map.of("fileName", String.valueOf(fileName)), null, cause);
    }
}
