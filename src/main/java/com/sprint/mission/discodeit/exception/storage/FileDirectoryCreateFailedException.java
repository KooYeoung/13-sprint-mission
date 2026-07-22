package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.nio.file.Path;

public class FileDirectoryCreateFailedException extends StorageException {
    public FileDirectoryCreateFailedException(Path path, Throwable cause) {
        super(ErrorCode.FILE_DIRECTORY_CREATE_FAILED, Map.of("path", path.toString()), cause);
    }
}
