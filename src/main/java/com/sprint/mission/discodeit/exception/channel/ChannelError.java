package com.sprint.mission.discodeit.exception.channel;

import lombok.Getter;

@Getter
public enum ChannelError {
    NOT_FOUND("존재하지 않는 채널 입니다."), PRIVATE_NOT_UPDATE("PRIVATE 채널은 수정할 수 없습니다.");

    private final String message;


    ChannelError(String message) {
        this.message = message;
    }
}
