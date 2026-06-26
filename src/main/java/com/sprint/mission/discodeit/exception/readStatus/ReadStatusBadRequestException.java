package com.sprint.mission.discodeit.exception.readStatus;

import com.sprint.mission.discodeit.exception.CustomBadRequestException;

public class ReadStatusBadRequestException extends CustomBadRequestException {

    public ReadStatusBadRequestException(String message) {
        super(message);
    }
}
