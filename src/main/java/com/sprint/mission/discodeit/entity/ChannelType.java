package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.channel.InvalidChannelTypeException;

public enum ChannelType {
    PUBLIC, PRIVATE;

    public static ChannelType getChannelType(String type) {
        if (type == null || type.isBlank()) {
            throw new InvalidChannelTypeException(ErrorCode.CHANNEL_TYPE_REQUIRED);
        }

        String normalizedType = type.trim().toUpperCase();


        try {
            return ChannelType.valueOf(normalizedType);
        } catch (IllegalArgumentException e) {
            throw new InvalidChannelTypeException(ErrorCode.CHANNEL_TYPE_INVALID);
        }

    }
}
