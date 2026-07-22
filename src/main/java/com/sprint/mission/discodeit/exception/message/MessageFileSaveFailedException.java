package com.sprint.mission.discodeit.exception.message;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;
import java.util.UUID;

public class MessageFileSaveFailedException extends MessageException {
    public MessageFileSaveFailedException(UUID messageId, int expectedCount, int insertedCount) {
        super(ErrorCode.MESSAGE_FILE_SAVE_FAILED, Map.of(
                "messageId", messageId,
                "expectedCount", expectedCount,
                "insertedCount", insertedCount
        ));
    }
}
