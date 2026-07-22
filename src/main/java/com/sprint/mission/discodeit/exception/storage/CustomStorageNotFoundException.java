package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class CustomStorageNotFoundException extends StorageException {
    public CustomStorageNotFoundException(UUID fileId) {
        super(ErrorCode.FILE_NOT_FOUND, Map.of("fileId", fileId));
    }

    public CustomStorageNotFoundException(Throwable cause) {
        super(ErrorCode.FILE_NOT_FOUND, cause);
    }

    public CustomStorageNotFoundException(UUID fileId, Throwable cause) {
        super(ErrorCode.FILE_NOT_FOUND, Map.of("fileId", fileId), cause);
    }
}
