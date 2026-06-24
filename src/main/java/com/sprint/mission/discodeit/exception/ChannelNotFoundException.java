package com.sprint.mission.discodeit.exception;

public class ChannelNotFoundException extends CustomNotFoundException {
    public ChannelNotFoundException() {
        super("존재하지 않는 채널 입니다.");
    }
}
