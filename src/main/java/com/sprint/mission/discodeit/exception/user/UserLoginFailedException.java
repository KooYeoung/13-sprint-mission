package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;

public class UserLoginFailedException extends UserException {
    public UserLoginFailedException() {
        super(ErrorCode.USER_LOGIN_FAILED);
    }
}
