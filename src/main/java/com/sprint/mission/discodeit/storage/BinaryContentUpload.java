package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.exception.storage.FileReadFailedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public record BinaryContentUpload(
        Path temporaryFile,
        String contentType,
        long contentLength
) implements AutoCloseable {

    public static BinaryContentUpload from(MultipartFile file) {
        return new BinaryContentUpload(
                createTemporaryFile(file),
                file.getContentType(),
                file.getSize()
        );
    }

    public InputStream inputStream() throws IOException {
        return Files.newInputStream(temporaryFile);
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(temporaryFile);
    }

    private static Path createTemporaryFile(MultipartFile file) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("discodeit-upload-", ".tmp");
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return temporaryFile;
        } catch (IOException | RuntimeException failure) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException | RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw new FileReadFailedException(file.getOriginalFilename(), failure);
        }
    }
}
