package com.sprint.mission.discodeit.dto.command.userStatus;

import com.sprint.mission.discodeit.dto.response.UserStatusDto;

import java.time.Instant;
import java.util.UUID;

public record UserStatusCreateCommand(
       Instant createdAt
) {

}
