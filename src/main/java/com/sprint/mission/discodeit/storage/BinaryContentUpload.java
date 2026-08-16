package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.exception.storage.FileReadFailedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public record BinaryContentUpload(
        InputStream inputStream,
        String contentType,
        long contentLength
) {

    public static BinaryContentUpload from(MultipartFile file) {
        return new BinaryContentUpload(
                getInputStreamFromFile(file),
                file.getContentType(),
                file.getSize()
        );
    }

    private static InputStream getInputStreamFromFile(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new FileReadFailedException(file.getOriginalFilename(), e);
        }
    }
}
