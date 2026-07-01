package com.sprint.mission.discodeit.exception.user;

import com.sprint.mission.discodeit.exception.CustomNotFoundException;

public class UserNotFoundException extends CustomNotFoundException {
    public UserNotFoundException() {
        super(UserError.NOT_FOUND.getMessage());
    }
}
