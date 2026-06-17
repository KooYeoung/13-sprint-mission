package com.sprint.mission.discodeit.dto.command.message;

import com.sprint.mission.discodeit.dto.response.MessageDto;
import lombok.With;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MessageCreateCommand(
        String content
        , UUID userId
        , UUID channelId
        , @With List<UUID> fileIds
) {

    public static MessageCreateCommand from(MessageDto messageDto){
        return new MessageCreateCommand(
                messageDto.content()
                ,messageDto.userId()
                ,messageDto.channelId()
                ,new ArrayList<>()
        );
    }
}
