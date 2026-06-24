package com.sprint.mission.discodeit.dto.request.message;

import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
import lombok.With;

import java.util.UUID;

public record MessageUpdateRequest(
      String content
      , UUID channelId
      , UUID userId
) {
    public MessageUpdateCommand toCommand(){
        return new MessageUpdateCommand(content);
    }
}
