package com.sprint.mission.discodeit.exception;

public class ChannelNotFoundException extends CustomNotFoundException {
    public ChannelNotFoundException() {
        super(ChannelError.NOT_FOUND.getMessage());
    }
}
