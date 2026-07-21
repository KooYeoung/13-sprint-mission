package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.exception.CustomInternalServerException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.file.CustomFileNotFoundException;
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
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(
        name = "discodeit.storage.type",
        havingValue = "local"
)
public class LocalBinaryContentStorage implements BinaryContentStorage {

    private final Path root;
    private final FileTransactionManager transactionManager;

    public LocalBinaryContentStorage(@Value("${discodeit.storage.local.root-path}") String rootPath, FileTransactionManager transactionManager) {
        this.root = Paths.get(rootPath);
        this.transactionManager = transactionManager;
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new CustomInternalServerException(ErrorCode.FILE_DIRECTORY_CREATE_FAILED, e);
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

            transactionManager.deleteOnRollback(savePath);

            return fileId;

        } catch (IOException e) {
            throw new CustomInternalServerException(ErrorCode.FILE_SAVE_FAILED, e);
        }
    }

    private @NonNull Path resolvePath(UUID fileId) {
        return root.resolve(fileId.toString());
    }

    @Override
    public InputStream get(UUID fileId) {
        Path savedPath = resolvePath(fileId);
        try {
            return Files.newInputStream(savedPath, StandardOpenOption.READ);
        } catch (NoSuchFileException e) {
            log.warn("파일이 존재하지 않습니다. fileId={}", fileId);
            throw new CustomFileNotFoundException(fileId, e);
        } catch (IOException e) {
            throw new CustomInternalServerException(ErrorCode.FILE_READ_FAILED, e);
        }
    }

    @Override
    public void delete(UUID fileId) {
        Path savedPath = resolvePath(fileId);
        transactionManager.deleteAfterCommit(savedPath);

    }

    @Override
    public Resource download(BinaryContentDto binaryContentDto) {
        return new InputStreamResource(get(binaryContentDto.id()));
    }

    @Override
    public void deleteAll(List<UUID> binaryContentIds) {
        binaryContentIds.forEach(this::delete);
    }
}
