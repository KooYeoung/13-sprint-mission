package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public class UserEmailDuplicatedException extends UserException {
    public UserEmailDuplicatedException(String email) {
        super(ErrorCode.USER_EMAIL_DUPLICATED, Map.of("email", email));
    }
}
