package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BinaryContentUploadEventListenerTest {
    @TempDir Path temporaryDirectory;
    @Mock BinaryContentUploadProcessor processor;
    @InjectMocks BinaryContentUploadEventListener listener;

    @Test
    void commitSubmitsUpload() throws IOException {
        var event = event(Files.createFile(temporaryDirectory.resolve("upload")));
        listener.uploadAfterCommit(event);
        verify(processor).upload(event);
        assertThat(event.contentUpload().temporaryFile()).exists();
    }

    @Test
    void rejectionDeletesTemporaryFileAndRecordsFailure() throws IOException {
        var event = event(Files.createFile(temporaryDirectory.resolve("upload")));
        var rejection = new TaskRejectedException("실행기 포화");
        doThrow(rejection).when(processor).upload(event);
        assertThatCode(() -> listener.uploadAfterCommit(event)).doesNotThrowAnyException();
        assertThat(event.contentUpload().temporaryFile()).doesNotExist();
        verify(processor).recordFailure(event.binaryContentId(), rejection);
    }

    @Test
    void rollbackDeletesTemporaryFileWithoutSubmittingAnyTask() throws IOException {
        Path file = Files.createFile(temporaryDirectory.resolve("upload"));
        listener.deleteTemporaryUploadAfterRollback(event(file));
        assertThat(file).doesNotExist();
        verifyNoInteractions(processor);
    }

    @Test
    void cleanupFailureDoesNotPreventFailureStatusOrInterruptRollback() throws IOException {
        // 비어 있지 않은 디렉터리는 deleteIfExists로 삭제할 수 없다.
        Files.createFile(temporaryDirectory.resolve("remaining"));
        var event = event(temporaryDirectory);
        var rejection = new TaskRejectedException("실행기 포화");
        doThrow(rejection).when(processor).upload(event);

        assertThatCode(() -> listener.uploadAfterCommit(event)).doesNotThrowAnyException();
        verify(processor).recordFailure(event.binaryContentId(), rejection);
        assertThatCode(() -> listener.deleteTemporaryUploadAfterRollback(event)).doesNotThrowAnyException();
        assertThat(temporaryDirectory.resolve("remaining")).exists();
    }

    private BinaryContentCreatedEvent event(Path file) {
        return new BinaryContentCreatedEvent(UUID.randomUUID(), new BinaryContentUpload(file, "text/plain", 1));
    }
}
