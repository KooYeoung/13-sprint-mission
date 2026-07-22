package com.sprint.mission.discodeit.exception.storage;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.nio.file.Path;
import java.util.Map;

public class FileSaveFailedException extends StorageException {
    public FileSaveFailedException(Path path, Throwable cause) {
        super(ErrorCode.FILE_SAVE_FAILED, Map.of("path", path.toString()), cause);
    }
}
