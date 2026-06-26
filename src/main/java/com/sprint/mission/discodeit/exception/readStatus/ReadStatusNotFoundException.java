package com.sprint.mission.discodeit.exception.readStatus;

import com.sprint.mission.discodeit.exception.CustomNotFoundException;

public class ReadStatusNotFoundException extends CustomNotFoundException {
    public ReadStatusNotFoundException() {
        super(ReadStatusError.NOT_FOUND.getMessage());
    }
}
