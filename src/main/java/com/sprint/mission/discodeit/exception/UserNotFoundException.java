package com.sprint.mission.discodeit.exception;

public class UserNotFoundException extends CustomNotFoundException {
    public UserNotFoundException() {
        super("존재하지 않는 유저 입니다.");
    }
}
