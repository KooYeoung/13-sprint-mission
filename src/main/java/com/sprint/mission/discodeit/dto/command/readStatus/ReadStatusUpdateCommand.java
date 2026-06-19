package com.sprint.mission.discodeit.dto.command.readStatus;

import java.time.Instant;

public record ReadStatusUpdateCommand(
Instant readAt
) {

}
