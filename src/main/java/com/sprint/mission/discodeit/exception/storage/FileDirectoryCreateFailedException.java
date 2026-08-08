package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.nio.file.Path;
import java.util.Map;

public class FileDirectoryCreateFailedException extends StorageException {
    public FileDirectoryCreateFailedException(Path path, Throwable cause) {
        super(
                ErrorCode.FILE_DIRECTORY_CREATE_FAILED,
                Map.of(),
                path,
                cause
        );
    }
}
