package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.exception.CustomInternalServerException;
import com.sprint.mission.discodeit.exception.file.CustomFileNotFoundException;
import com.sprint.mission.discodeit.exception.file.FileError;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BinaryContentService {
    private final BinaryContentRepository binaryContentRepository;
    private final BinaryContentStorage binaryContentStorage;

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

        return Optional.of(binaryContent);
    }

    @Transactional(readOnly = true)
    public BinaryContentDto findById(UUID id) {

        BinaryContent binaryContent = binaryContentRepository.findById(id)
                .orElseThrow(CustomFileNotFoundException::new);

        return BinaryContentDto.from(binaryContent, getBytes(binaryContent));
    }


    @Transactional(readOnly = true)
    public List<BinaryContentDto> findAllByIdIn(List<UUID> ids) {

        return binaryContentRepository
                .findAllByIdIn(ids)
                .stream()
                .map(b -> BinaryContentDto.from(b, getBytes(b)))
                .toList();
    }

    public void delete(UUID id) {

        Optional<BinaryContent> existingContent = binaryContentRepository.findById(id);

        if (existingContent.isEmpty()) {
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

    @NonNull
    private byte[] getBytes(BinaryContent binaryContent) {
        try {
            return binaryContentStorage.get(binaryContent.getId())
                    .readAllBytes();
        } catch (NoSuchFileException e) {
            throw new CustomFileNotFoundException();
        } catch (IOException e) {
            throw new CustomInternalServerException(FileError.READ.getMessage(), e);
        }
    }

    private byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new CustomInternalServerException(FileError.READ.getMessage(), e);
        }
    }
}
