package com.sprint.mission.discodeit.exception.message;

import com.sprint.mission.discodeit.exception.ErrorCode;

import java.util.Map;

public class MessageInvalidPagingConditionException extends MessageException {

    public MessageInvalidPagingConditionException(String parameter) {
        super(ErrorCode.MESSAGE_INVALID_PAGING_CONDITION, Map.of("parameter", parameter));
    }
}
