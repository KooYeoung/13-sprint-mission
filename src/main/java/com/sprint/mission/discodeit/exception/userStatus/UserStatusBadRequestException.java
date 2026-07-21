package com.sprint.mission.discodeit.exception.userStatus;

import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.UserStatusException;

import java.util.Map;
import java.util.UUID;

public class UserStatusBadRequestException extends UserStatusException {

    public UserStatusBadRequestException(ErrorCode errorCode, UUID userId) {
        super(errorCode, Map.of("userId", userId));
    }
}
