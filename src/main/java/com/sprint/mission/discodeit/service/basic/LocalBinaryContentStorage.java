package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.exception.storage.FileDirectoryCreateFailedException;
import com.sprint.mission.discodeit.exception.storage.FileNotFoundException;
import com.sprint.mission.discodeit.exception.storage.FileReadFailedException;
import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import com.sprint.mission.discodeit.storage.DownloadResult;
import com.sprint.mission.discodeit.storage.ResourceDownloadResult;
import com.sprint.mission.discodeit.storage.type.LocalProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(
        name = "discodeit.storage.type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalBinaryContentStorage implements BinaryContentStorage {

    private final Path root;
    private final StorageTransactionManager transactionManager;

    public LocalBinaryContentStorage(LocalProperties local, StorageTransactionManager transactionManager) {
        this.root = Paths.get(local.rootPath());
        this.transactionManager = transactionManager;
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new FileDirectoryCreateFailedException(root, e);
        }
    }

    @Override
    public UUID put(UUID fileId, BinaryContentUpload upload) {

        Path savePath = resolvePath(fileId);
        boolean fileCreated = false;
        try {

            try (
                    OutputStream os = Files.newOutputStream(savePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    InputStream is = upload.inputStream()
            ) {
                fileCreated = true;
                is.transferTo(os);
            }

            transactionManager.afterRollback(
                    "로컬 파일 업로드 롤백 정리. path=" + savePath,
                    () -> Files.deleteIfExists(savePath)
            );

            return fileId;
        } catch (IOException e) {
            if (fileCreated) {
                try {
                    Files.deleteIfExists(savePath);
                } catch (IOException deleteException) {
                    e.addSuppressed(deleteException);
                }
            }
            throw new FileSaveFailedException(savePath, e);
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
            throw new FileNotFoundException(fileId, e);
        } catch (IOException e) {
            throw new FileReadFailedException(fileId, savedPath, e);
        }
    }

    @Override
    public void delete(UUID fileId) {
        Path savedPath = resolvePath(fileId);

        transactionManager.afterCommit(
                "로컬 파일 커밋 후 삭제. path=" + savedPath,
                () -> Files.deleteIfExists(savedPath)
        );
    }

    @Override
    public DownloadResult download(BinaryContentDto binaryContentDto) {
        return new ResourceDownloadResult(new PathResource(resolvePath(binaryContentDto.id())));
    }

    @Override
    public void deleteAll(List<UUID> binaryContentIds) {
        binaryContentIds.forEach(this::delete);
    }
}
