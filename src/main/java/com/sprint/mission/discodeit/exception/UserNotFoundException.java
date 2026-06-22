package com.sprint.mission.discodeit.exception;

public class UserNotFoundException extends CustomNotFoundException {
    public UserNotFoundException() {
        super("유저가 존재하지 않습니다.");
    }
}
