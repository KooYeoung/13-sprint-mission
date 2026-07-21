package com.sprint.mission.discodeit.exception.file;

import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.FileException;

import java.util.Map;
import java.util.UUID;

public class CustomFileNotFoundException extends FileException {
    public CustomFileNotFoundException(UUID fileId) {
        super(ErrorCode.FILE_NOT_FOUND, Map.of("fileId", fileId));
    }

    public CustomFileNotFoundException(Throwable cause) {
        super(ErrorCode.FILE_NOT_FOUND, cause);
    }

    public CustomFileNotFoundException(UUID fileId, Throwable cause) {
        super(ErrorCode.FILE_NOT_FOUND, Map.of("fileId", fileId), cause);
    }
}
