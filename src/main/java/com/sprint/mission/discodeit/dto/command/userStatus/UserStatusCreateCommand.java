package com.sprint.mission.discodeit.dto.command.userStatus;

import com.sprint.mission.discodeit.dto.response.UserStatusDto;

import java.time.Instant;
import java.util.UUID;

public record UserStatusCreateCommand(
        UUID userId
        , Instant createdAt
) {
    public static UserStatusCreateCommand from(UserStatusDto dto){
        return new UserStatusCreateCommand(dto.userId(), Instant.now());
    }
}
