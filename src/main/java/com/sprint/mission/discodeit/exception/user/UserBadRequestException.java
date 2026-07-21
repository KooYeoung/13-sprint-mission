package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.UserException;

public class UserBadRequestException extends UserException {
    public UserBadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

}
