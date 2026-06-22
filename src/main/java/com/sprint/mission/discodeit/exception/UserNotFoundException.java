package com.sprint.mission.discodeit.exception;

public class UserNotFoundException extends CustomNotFoundException {
    public UserNotFoundException() {
        super(UserError.NOT_FOUND.getMessage());
    }
}
