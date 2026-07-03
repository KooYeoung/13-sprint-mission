package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.MessageFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        config = MapStructConfig.class,
        uses = {UserMapper.class, MessageAttachmentMapper.class, DateTimeMapper.class}
)
public interface MessageMapper {

    @Mapping(source = "message.id", target = "id")
    @Mapping(source = "message.createdAt", target = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "message.updatedAt", target = "updatedAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "message.content", target = "content")
    @Mapping(source = "message.channelId", target = "channelId")
    @Mapping(source = "message.author", target = "author")
    @Mapping(source = "messageFiles", target = "attachments", qualifiedByName = "toAttachments")
    MessageDto toDto(Message message, List<MessageFile> messageFiles);

    default MessageDto toDto(Message message) {
        return toDto(
                message,
                message.getMessageFiles()
        );
    }


}
