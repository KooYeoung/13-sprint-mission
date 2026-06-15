package com.sprint.mission.discodeit.exception;

public class MessageNotFountException extends CustomNotFoundException {
    public MessageNotFountException() {
        super("존재하지 않는 메시지 입니다.");
    }
}
