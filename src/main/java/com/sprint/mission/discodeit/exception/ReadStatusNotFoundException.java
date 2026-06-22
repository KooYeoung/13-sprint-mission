package com.sprint.mission.discodeit.exception;

public class ReadStatusNotFoundException extends CustomNotFoundException {
    public ReadStatusNotFoundException() {
        super(ReadStatusError.NOT_FOUND.getMessage());
    }
}
