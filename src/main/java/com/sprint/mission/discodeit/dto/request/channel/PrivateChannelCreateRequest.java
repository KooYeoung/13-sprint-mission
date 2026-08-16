package com.sprint.mission.discodeit.dto.request.channel;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePrivateCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import com.sprint.mission.discodeit.entity.ChannelType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;
import java.util.UUID;

public record PrivateChannelCreateRequest(
        @NotNull(message = ValidationMessage.NOT_NULL_MESSAGE)
        @Size(min = 2, message = ValidationMessage.PARTICIPANTS_SIZE_MESSAGE)
        @UniqueElements(message = ValidationMessage.USER_ID_UNIQUE_MESSAGE)
        List<@NotNull(message = ValidationMessage.NOT_NULL_MESSAGE)
                UUID> participantIds
) {
    public ChannelCreatePrivateCommand toCommand() {
        return new ChannelCreatePrivateCommand(participantIds, ChannelType.PRIVATE);
    }
}