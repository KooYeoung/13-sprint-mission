package com.sprint.mission.discodeit.exception.userStatus;

import com.sprint.mission.discodeit.exception.CustomNotFoundException;

public class UserStatusNotFoundException extends CustomNotFoundException {

    public UserStatusNotFoundException() {
        super("유저 상태가 존재하지 않습니다.");
    }
}
