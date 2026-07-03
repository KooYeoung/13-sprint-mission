package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.MessageFile;
import com.sprint.mission.discodeit.exception.CustomInternalServerException;
import com.sprint.mission.discodeit.repository.MessageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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

    public List<MessageFile> save(Message message, List<MultipartFile> files) {

        if (files == null || files.isEmpty()) return List.of();

        List<UUID> fileIds = new ArrayList<>();

        for (MultipartFile file : files) {
            Optional<BinaryContent> binaryContent = binaryContentService.create(file);
            binaryContent.ifPresent(b -> fileIds.add(b.getId()));
        }

        if (fileIds.isEmpty()) return List.of();

        int insertedCount = messageFileRepository.bulkInsert(fileIds, message.getId());

        if (insertedCount != fileIds.size()) throw new CustomInternalServerException("메시지 파일 저장에 실패했습니다.");

        return messageFileRepository.findAllByMessage_Id(message.getId());
    }

    public void deleteByMessageId(UUID messageId) {
        if (!messageFileRepository.existsByMessage_Id(messageId)) return;

        List<MessageFile> messageFiles = messageFileRepository.findAllByMessage_Id(messageId);

        List<BinaryContent> binaryContents = convertToBinaryContents(messageFiles);

        binaryContentService.deleteAll(binaryContents);

        messageFileRepository.deleteByMessage_Id(messageId);
    }

    public void deleteAllByChannelId(UUID channelId) {
        if (!messageFileRepository.existsByMessage_Channel_Id(channelId)) return;

        List<MessageFile> messageFiles = messageFileRepository.findAllByChannelId(channelId);

        List<BinaryContent> binaryContents = convertToBinaryContents(messageFiles);

        binaryContentService.deleteAll(binaryContents);

        List<UUID> messageFileIds = messageFiles.stream().map(MessageFile::getId).toList();

        messageFileRepository.deleteAllByIdIn(messageFileIds);
    }

    @Transactional(readOnly = true)
    public List<MessageFile> findAllByMessageId(UUID messageId) {
        return messageFileRepository.findAllByMessage_Id(messageId);
    }

    @Transactional(readOnly = true)
    public List<MessageFile> findAllByMessageIds(List<UUID> messageIds) {
        return messageFileRepository.findAllByMessage_IdIn(messageIds);
    }

    private @NonNull List<BinaryContent> convertToBinaryContents(List<MessageFile> messageFiles) {
        return messageFiles
                .stream()
                .map(MessageFile::getBinaryContent)
                .toList();
    }
}
