package com.sprint.mission.discodeit.exception.channel;

import com.sprint.mission.discodeit.exception.CustomNotFoundException;

public class ChannelNotFoundException extends CustomNotFoundException {
    public ChannelNotFoundException() {
        super(ChannelError.NOT_FOUND.getMessage());
    }
}
