package com.sprint.mission.discodeit.dto.command.userStatus;

import java.time.Instant;

public record UserStatusCreateCommand(
       Instant createdAt
) {

}
