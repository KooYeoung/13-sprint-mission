package com.sprint.mission.discodeit.exception.message;

import com.sprint.mission.discodeit.exception.CustomNotFoundException;

public class MessageNotFoundException extends CustomNotFoundException {
    public MessageNotFoundException() {
        super("존재하지 않는 메시지 입니다.");
    }
}
