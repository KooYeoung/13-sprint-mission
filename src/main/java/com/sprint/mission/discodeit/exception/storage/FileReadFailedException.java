package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class FileReadFailedException extends StorageException {
    public FileReadFailedException(UUID fileId, Throwable cause) {
        super(ErrorCode.FILE_READ_FAILED, Map.of("fileId", fileId), cause);
    }

    public FileReadFailedException(String fileName, Throwable cause) {
        super(ErrorCode.FILE_READ_FAILED, Map.of("fileName", String.valueOf(fileName)), cause);
    }
}
