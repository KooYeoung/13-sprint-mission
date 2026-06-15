package com.sprint.mission.discodeit.exception;

public class LoginFailException extends CustomBadRequestException {
    public LoginFailException(String message) {
        super(message);
    }
}
