package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.exception.channel.InvalidChannelTypeException;

public enum ChannelType {
    PUBLIC, PRIVATE;

    public static ChannelType getChannelType(String type) {
        if (type == null || type.isBlank()) throw new InvalidChannelTypeException("타입이 존재하지 않습니다.");

        String normalizedType = type.trim().toUpperCase();


        try {
            return ChannelType.valueOf(normalizedType);
        } catch (IllegalArgumentException e) {
            throw new InvalidChannelTypeException("지원하지 않는 타입입니다.");
        }

    }
}
