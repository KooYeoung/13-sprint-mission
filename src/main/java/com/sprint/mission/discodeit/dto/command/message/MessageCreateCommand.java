package com.sprint.mission.discodeit.dto.command.message;

import java.util.UUID;

public record MessageCreateCommand(
        String content,
        UUID userId,
        UUID channelId
) {

}
