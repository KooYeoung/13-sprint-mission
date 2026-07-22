package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class FileNotFoundException extends StorageException {
    public FileNotFoundException(UUID fileId) {
        super(ErrorCode.FILE_NOT_FOUND, Map.of("fileId", fileId));
    }

    public FileNotFoundException(UUID fileId, Throwable cause) {
        super(ErrorCode.FILE_NOT_FOUND, Map.of("fileId", fileId), cause);
    }
}
