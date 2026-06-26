package com.sprint.mission.discodeit.exception.userStatus;

import com.sprint.mission.discodeit.exception.CustomBadRequestException;

public class UserStatusBadRequestException extends CustomBadRequestException {

    public UserStatusBadRequestException(String message) {
        super(message);
    }
}
