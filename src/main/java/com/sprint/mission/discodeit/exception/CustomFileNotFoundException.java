package com.sprint.mission.discodeit.exception;

public class CustomFileNotFoundException extends CustomNotFoundException {
    public CustomFileNotFoundException() {
        super(FileError.NOT_FOUND.getMessage());
    }
}
