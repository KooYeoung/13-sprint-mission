package com.sprint.mission.discodeit.exception;

import lombok.Getter;

@Getter
public enum UserError {
    EMAIL("이미 존재하는 이메일 입니다."),
    USERNAME("이미 존재하는 아이디 입니다."),
    NOT_FOUND("유저가 존재하지 않습니다."),
    LOGIN("아이디 또는 비밀번호가 일치하지 않습니다.");

    private final String message;

    UserError(String message) {
        this.message = message;
    }
}
