package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.exception.CustomInternalServerException;
import com.sprint.mission.discodeit.exception.file.FileError;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(
        name = "discodeit.storage.type",
        havingValue = "local"
)
public class LocalBinaryContentStorage implements BinaryContentStorage {

    private final Path root;

    public LocalBinaryContentStorage(@Value("${discodeit.storage.local.root-path}") String rootPath) {
        this.root = Paths.get(rootPath);
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new CustomInternalServerException(FileError.DIRECTORY.getMessage(), e);
        }
    }

    @Override
    public UUID put(UUID fileId, byte[] bytes) {
        Path savePath = resolvePath(fileId);
        try {
            Files.write(
                    savePath,
                    bytes,
                    StandardOpenOption.CREATE_NEW
            );

            return fileId;

        } catch (IOException e) {
            throw new CustomInternalServerException(FileError.SAVE.getMessage(), e);
        }
    }

    private @NonNull Path resolvePath(UUID fileId) {
        return root.resolve(fileId.toString());
    }

    @Override
    public InputStream get(UUID fileId) {
        Path savePath = resolvePath(fileId);
        try {
            return Files.newInputStream(savePath, StandardOpenOption.READ);
        } catch (NoSuchFileException e) {
            throw new CustomInternalServerException(FileError.NOT_FOUND.getMessage(), e);
        } catch (IOException e) {
            throw new CustomInternalServerException(FileError.READ.getMessage(), e);
        }
    }

    @Override
    public void delete(UUID fileId) {
        Path savedPath = resolvePath(fileId);
        try {
            Files.deleteIfExists(savedPath);
        } catch (IOException e) {
            throw new CustomInternalServerException(FileError.DELETE.getMessage(), e);
        }

    }

    @Override
    public Resource download(BinaryContentDto binaryContentDto) {

        return new InputStreamResource(get(binaryContentDto.id()));
    }
}
