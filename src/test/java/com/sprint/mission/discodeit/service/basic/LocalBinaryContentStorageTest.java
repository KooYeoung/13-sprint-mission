package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import com.sprint.mission.discodeit.storage.type.LocalProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LocalBinaryContentStorageTest {
    @TempDir Path root;

    @Test
    void interruptedDelayPreservesInterruptWithoutWritingDestination() throws IOException {
        var storage = storage(Duration.ofSeconds(3));
        var upload = upload();
        UUID id = UUID.randomUUID();
        try {
            Thread.currentThread().interrupt();
            assertThatThrownBy(() -> storage.put(id, upload))
                    .isInstanceOf(FileSaveFailedException.class).hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(root.resolve(id.toString())).doesNotExist();
            assertThat(upload.temporaryFile()).exists();
        } finally { Thread.interrupted(); }
    }

    @Test
    void failureOpeningDestinationPreservesExistingFile() throws Exception {
        UUID id = UUID.randomUUID();
        Files.write(root.resolve(id.toString()), new byte[]{9});
        var upload = upload();
        assertThatThrownBy(() -> storage(Duration.ZERO).put(id, upload)).isInstanceOf(FileSaveFailedException.class);
        assertThat(upload.temporaryFile()).exists();
        assertThat(Files.readAllBytes(root.resolve(id.toString()))).containsExactly((byte) 9);
    }

    @Test
    void successfulUploadPersistsBytesAndLeavesTemporaryFileForCallerCleanup() throws Exception {
        UUID id = UUID.randomUUID();
        var upload = upload();
        storage(Duration.ZERO).put(id, upload);
        assertThat(upload.temporaryFile()).exists();
        assertThat(Files.readAllBytes(root.resolve(id.toString()))).containsExactly(1, 2, 3);
    }

    private LocalBinaryContentStorage storage(Duration delay) {
        return new LocalBinaryContentStorage(new LocalProperties(root.toString(), delay), mock(StorageTransactionManager.class));
    }

    private BinaryContentUpload upload() throws IOException {
        Path file = Files.write(root.resolve("source"), new byte[]{1, 2, 3});
        return new BinaryContentUpload(file, "text/plain", 3);
    }
}
