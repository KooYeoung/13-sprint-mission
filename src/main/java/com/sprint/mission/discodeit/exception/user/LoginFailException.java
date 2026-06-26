package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.CustomBadRequestException;

public class LoginFailException extends CustomBadRequestException {
    public LoginFailException(String message) {
        super(message);
    }
}
