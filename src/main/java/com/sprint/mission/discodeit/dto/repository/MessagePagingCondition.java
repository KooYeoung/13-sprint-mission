package com.sprint.mission.discodeit.dto.repository;

import com.sprint.mission.discodeit.exception.message.MessageInvalidPagingConditionException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record MessagePagingCondition(
        UUID channelId,
        Pageable pageable,
        @Nullable UUID cursor
) {

    public MessagePagingCondition {
        if (channelId == null) {
            throw new MessageInvalidPagingConditionException("channelId");
        }
        if (pageable == null) {
            throw new MessageInvalidPagingConditionException("pageable");
        }
    }
}
