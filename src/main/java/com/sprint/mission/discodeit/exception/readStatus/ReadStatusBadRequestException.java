package com.sprint.mission.discodeit.exception.readStatus;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class ReadStatusBadRequestException extends ReadStatusException {

    public ReadStatusBadRequestException(ErrorCode errorCode, UUID channelId, UUID userId) {
        super(errorCode, Map.of(
                "channelId", channelId,
                "userId", userId
        ));
    }

}
