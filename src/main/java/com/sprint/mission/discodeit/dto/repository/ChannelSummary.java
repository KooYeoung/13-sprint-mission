package com.sprint.mission.discodeit.dto.repository;

import com.sprint.mission.discodeit.entity.ChannelType;

import java.time.Instant;
import java.util.UUID;

public record ChannelSummary(
        UUID id,
        ChannelType type,
        String name,
        String description,
        Instant lastMessageAt
) {
}
