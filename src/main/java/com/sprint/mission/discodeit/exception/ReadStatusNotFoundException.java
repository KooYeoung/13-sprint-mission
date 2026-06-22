package com.sprint.mission.discodeit.exception;

public class ReadStatusNotFoundException extends CustomNotFoundException {
    public ReadStatusNotFoundException() {
        super("읽음 상태가 존재하지 않습니다.");
    }
}
