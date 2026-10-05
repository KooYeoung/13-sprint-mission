package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import com.sprint.mission.discodeit.storage.type.LocalProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
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
    void interruptedDelayClosesStreamAndPreservesInterrupt() {
        var storage = storage(Duration.ofSeconds(3));
        var stream = new TrackingStream();
        try {
            Thread.currentThread().interrupt();
            assertThatThrownBy(() -> storage.put(UUID.randomUUID(), upload(stream)))
                    .isInstanceOf(FileSaveFailedException.class).hasCauseInstanceOf(InterruptedException.class);
            assertThat(stream.closed).isTrue();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }

    @Test
    void failureOpeningDestinationClosesInputAndPreservesExistingFile() throws Exception {
        UUID id = UUID.randomUUID();
        Files.write(root.resolve(id.toString()), new byte[]{9});
        var stream = new TrackingStream();
        assertThatThrownBy(() -> storage(Duration.ZERO).put(id, upload(stream))).isInstanceOf(FileSaveFailedException.class);
        assertThat(stream.closed).isTrue();
        assertThat(Files.readAllBytes(root.resolve(id.toString()))).containsExactly((byte) 9);
    }

    @Test
    void successfulUploadClosesStreamAndPersistsBytes() throws Exception {
        UUID id = UUID.randomUUID();
        var stream = new TrackingStream();
        storage(Duration.ZERO).put(id, upload(stream));
        assertThat(stream.closed).isTrue();
        assertThat(Files.readAllBytes(root.resolve(id.toString()))).containsExactly(1, 2, 3);
    }

    private LocalBinaryContentStorage storage(Duration delay) {
        return new LocalBinaryContentStorage(new LocalProperties(root.toString(), delay), mock(StorageTransactionManager.class));
    }

    private BinaryContentUpload upload(TrackingStream stream) { return new BinaryContentUpload(stream, "text/plain", 3); }

    private static class TrackingStream extends ByteArrayInputStream {
        boolean closed;
        TrackingStream() { super(new byte[]{1, 2, 3}); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
}
