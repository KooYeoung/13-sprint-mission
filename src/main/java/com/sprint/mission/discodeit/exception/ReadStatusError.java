package com.sprint.mission.discodeit.exception;

import lombok.Getter;

@Getter
public enum ReadStatusError {
    IS_PRIVATE_CHANNEL("비공개 채널만 등록 가능합니다."), NOT_FOUND("읽음 상태가 존재하지 않습니다."), HAS_READ("이미 읽음 상태가 존재합니다.") ;
    private final String message;

    ReadStatusError(String message) {
        this.message = message;
    }
}
