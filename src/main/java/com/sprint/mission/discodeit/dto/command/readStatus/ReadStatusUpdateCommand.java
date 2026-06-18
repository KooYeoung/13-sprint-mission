package com.sprint.mission.discodeit.dto.command.readStatus;

import com.sprint.mission.discodeit.dto.response.ReadStatusDto;

import java.time.Instant;

public record ReadStatusUpdateCommand(
Instant readAt
) {

    public static ReadStatusUpdateCommand from(ReadStatusDto dto){
        return new ReadStatusUpdateCommand(
         dto.readAt()
        );
    }
}
