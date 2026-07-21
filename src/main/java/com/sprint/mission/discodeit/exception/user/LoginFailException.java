package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.UserException;

public class LoginFailException extends UserException {
    public LoginFailException() {
        super(ErrorCode.USER_LOGIN_FAILED);
    }
}
