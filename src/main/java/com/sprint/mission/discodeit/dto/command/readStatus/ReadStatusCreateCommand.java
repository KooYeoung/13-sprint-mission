package com.sprint.mission.discodeit.dto.command.readStatus;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusCreateCommand(
        UUID userId,
        Instant readAt
) {

}
