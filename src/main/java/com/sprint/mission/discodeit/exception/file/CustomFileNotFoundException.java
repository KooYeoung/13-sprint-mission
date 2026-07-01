package com.sprint.mission.discodeit.exception.file;

import com.sprint.mission.discodeit.exception.CustomNotFoundException;

public class CustomFileNotFoundException extends CustomNotFoundException {
    public CustomFileNotFoundException() {
        super(FileError.NOT_FOUND.getMessage());
    }
}
