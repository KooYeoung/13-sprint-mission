package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.MessageFile;
import com.sprint.mission.discodeit.repository.MessageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageFileService {
    private final MessageFileRepository messageFileRepository;
    private final BinaryContentService binaryContentService;

    public List<MessageFile> save(Message message, List<MultipartFile> files){

        List<MessageFile> messageFiles = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                Optional<BinaryContent> binaryContent = binaryContentService.create(file);
                if (binaryContent.isEmpty()) {
                    continue;
                }
                BinaryContent messageFile = binaryContent.get();
                messageFiles.add(new MessageFile(message, messageFile));
            }
        }

        // 벌크 처리.. 필요
        return messageFileRepository.saveAll(messageFiles);
    }

    public void deleteByMessageId(UUID messageId){
        if(!messageFileRepository.existsByMessage_Id(messageId)) return;

        messageFileRepository.findAllByMessage_Id(messageId).forEach(messageFile -> {
            binaryContentService.delete(messageFile.getBinaryContent());
        });

        messageFileRepository.deleteByMessage_Id(messageId);
    }

    public void deleteAll(List<MessageFile> messageFiles){
        messageFiles.forEach(messageFile -> {
            binaryContentService.delete(messageFile.getBinaryContent());
        });

        messageFileRepository.deleteAll(messageFiles);
    }
}
