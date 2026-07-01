package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.CustomBadRequestException;

public class UserBadRequestException extends CustomBadRequestException {
    public UserBadRequestException(String message) {
        super(message);
    }
}
