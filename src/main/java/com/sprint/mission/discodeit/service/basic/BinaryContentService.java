package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.aspect.LogAction;
import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.DownloadDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.exception.storage.FileNotFoundException;
import com.sprint.mission.discodeit.exception.storage.FileReadFailedException;
import com.sprint.mission.discodeit.mapper.BinaryContentMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BinaryContentService {
    private final BinaryContentRepository binaryContentRepository;
    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentMapper binaryContentMapper;

    public Optional<BinaryContent> create(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }

        String originalFileName = file.getOriginalFilename();

        BinaryContent binaryContent = binaryContentRepository.save(new BinaryContent(
                originalFileName,
                file.getContentType(),
                file.getSize()
        ));

        binaryContentStorage.put(binaryContent.getId(), getBytes(file));

        log.info("파일 업로드 완료. binaryContentId={}", binaryContent.getId());
        return Optional.of(binaryContent);
    }

    @Transactional(readOnly = true)
    public BinaryContentDto findById(UUID id) {

        BinaryContent binaryContent = getBinaryContentById(id);

        return binaryContentMapper.toDto(binaryContent);
    }


    @Transactional(readOnly = true)
    public List<BinaryContentDto> findAllByIdIn(List<UUID> ids) {

        return binaryContentRepository
                .findAllByIdIn(ids)
                .stream()
                .map(binaryContentMapper::toDto)
                .toList();
    }

    public void delete(UUID id) {

        Optional<BinaryContent> existingContent = binaryContentRepository.findById(id);

        if (existingContent.isEmpty()) {
            log.warn("파일이 존재하지 않습니다. binaryContentId={}", id);
            return;
        }

        BinaryContent binaryContent = existingContent.get();

        binaryContentStorage.delete(binaryContent.getId());

        binaryContentRepository.deleteById(id);

    }

    public void delete(BinaryContent binaryContent) {

        if (binaryContent == null) return;

        binaryContentStorage.delete(binaryContent.getId());

        binaryContentRepository.deleteById(binaryContent.getId());

    }

    public void deleteAll(List<BinaryContent> binaryContents) {
        if (binaryContents == null || binaryContents.isEmpty()) return;

        List<UUID> binaryContentIds = binaryContents.stream().map(BinaryContent::getId).toList();

        binaryContentStorage.deleteAll(binaryContentIds);

        binaryContentRepository.deleteAllByIdIn(binaryContentIds);
    }

    @LogAction(value = "파일 다운로드")
    public DownloadDto download(UUID binaryContentId) {
        BinaryContent binaryContent = getBinaryContentById(binaryContentId);
        Resource resource = binaryContentStorage.download(binaryContentMapper.toDto(binaryContent));

        return binaryContentMapper.toDownloadDto(binaryContent, resource);
    }

    private byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new FileReadFailedException(file.getOriginalFilename(), e);
        }
    }

    private @NonNull BinaryContent getBinaryContentById(UUID id) {
        return binaryContentRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException(id));
    }
}
