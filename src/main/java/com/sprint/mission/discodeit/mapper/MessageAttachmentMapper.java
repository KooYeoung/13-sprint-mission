package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.entity.MessageFile;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageAttachmentMapper {

    private final BinaryContentMapper binaryContentMapper;

    @Named("toAttachments")
    public List<BinaryContentDto> toDtos(List<MessageFile> messageFiles) {
        if (messageFiles == null || messageFiles.isEmpty()) {
            return List.of();
        }

        return messageFiles.stream()
                .map(MessageFile::getBinaryContent)
                .map(binaryContentMapper::toDto)
                .toList();
    }
}
