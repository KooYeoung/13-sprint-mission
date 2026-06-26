package com.sprint.mission.discodeit.exception;

import com.sprint.mission.discodeit.exception.file.FileError;

public class CustomFileNotFoundException extends CustomNotFoundException {
    public CustomFileNotFoundException() {
        super(FileError.NOT_FOUND.getMessage());
    }
}
