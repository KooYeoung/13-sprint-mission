package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.exception.storage.FileReadFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class BinaryContentUploadTest {
    @TempDir Path directory;

    @Test
    void independentTemporaryFilesCanBeReopenedAfterOriginalRequestFileIsDeleted() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3};
        Path original = Files.write(directory.resolve("request"), bytes);
        var file = new MockMultipartFile("file", "same-name.txt", "text/plain", bytes) {
            @Override public InputStream getInputStream() throws IOException {
                return Files.newInputStream(original);
            }
        };
        Path firstPath;
        Path secondPath;
        try (var first = BinaryContentUpload.from(file); var second = BinaryContentUpload.from(file)) {
            firstPath = first.temporaryFile();
            secondPath = second.temporaryFile();
            assertThat(firstPath).isNotEqualTo(secondPath);
            Files.delete(original);
            for (var upload : new BinaryContentUpload[]{first, first, second}) {
                try (var input = upload.inputStream()) {
                    assertThat(input.readAllBytes()).isEqualTo(bytes);
                }
            }
        }
        assertThat(firstPath).doesNotExist();
        assertThat(secondPath).doesNotExist();
    }

    @Test
    void copyFailureClosesRequestStreamAndDeletesPartialTemporaryFile() throws Exception {
        Path destination = Files.createFile(directory.resolve("partial"));
        var closed = new AtomicBoolean();
        var failure = new IOException("파일 복사 중 읽기 실패");
        var file = new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1, 2}) {
            @Override public InputStream getInputStream() {
                return new InputStream() {
                    private boolean firstByte = true;
                    @Override public int read() throws IOException {
                        if (firstByte) {
                            firstByte = false;
                            return 1;
                        }
                        throw failure;
                    }
                    @Override public void close() { closed.set(true); }
                };
            }
        };
        // 임시 파일의 목적지만 고정하고 실제 복사·닫기·삭제를 실행한다.
        try (var files = mockStatic(Files.class, invocation -> {
            if (invocation.getMethod().getName().equals("createTempFile")) return destination;
            return invocation.callRealMethod();
        })) {
            assertThatThrownBy(() -> BinaryContentUpload.from(file))
                    .isInstanceOf(FileReadFailedException.class).hasCause(failure);
        }
        assertThat(destination).doesNotExist();
        assertThat(closed).isTrue();
    }
}
