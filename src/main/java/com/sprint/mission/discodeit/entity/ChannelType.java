package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.exception.channel.ChannelTypeInvalidException;
import com.sprint.mission.discodeit.exception.channel.ChannelTypeRequiredException;

public enum ChannelType {
    PUBLIC, PRIVATE;

    public static ChannelType getChannelType(String type) {
        if (type == null || type.isBlank()) {
            throw new ChannelTypeRequiredException();
        }

        String normalizedType = type.trim().toUpperCase();


        try {
            return ChannelType.valueOf(normalizedType);
        } catch (IllegalArgumentException e) {
            throw new ChannelTypeInvalidException(type);
        }

    }
}
