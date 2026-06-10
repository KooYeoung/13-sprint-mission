package com.sprint.mission.discodeit.dto.command.message;

import com.sprint.mission.discodeit.dto.response.MessageDto;

public record MessageUpdateCommand(
        String content
) {

    public static MessageUpdateCommand from(MessageDto dto){
        return new MessageUpdateCommand(dto.content());
    }
}
