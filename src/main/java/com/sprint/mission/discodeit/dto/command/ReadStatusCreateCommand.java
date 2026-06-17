package com.sprint.mission.discodeit.dto.command;

import com.sprint.mission.discodeit.dto.response.ReadStatusDto;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusCreateCommand(
        UUID userId
        , UUID channelId
        , Instant readAt
) {

    public static ReadStatusCreateCommand from(ReadStatusDto dto){
        return new ReadStatusCreateCommand(
                dto.userId()
                ,dto.channelId()
                ,dto.readAt()
        );
    }
}
