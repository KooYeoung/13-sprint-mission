package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BinaryContentUploadEventListenerTest {
    @Mock BinaryContentUploadProcessor processor;
    @InjectMocks BinaryContentUploadEventListener listener;

    @Test
    void commitSubmitsUpload() {
        var event = event(new TrackingStream());
        listener.uploadAfterCommit(event);
        verify(processor).upload(event);
    }

    @Test
    void rejectionClosesStreamAndRecordsFailureEvenWhenClosingFails() {
        var stream = new TrackingStream() {
            @Override public void close() throws IOException {
                super.close();
                throw new IOException("닫기 실패");
            }
        };
        var event = event(stream);
        var rejection = new TaskRejectedException("실행기 포화");
        doThrow(rejection).when(processor).upload(event);
        assertThatCode(() -> listener.uploadAfterCommit(event)).doesNotThrowAnyException();
        assertThat(stream.closed).isTrue();
        verify(processor).recordFailure(event.binaryContentId(), rejection);
    }

    @Test
    void rollbackClosesStreamWithoutSubmittingAnyTask() {
        var stream = new TrackingStream();
        listener.closeUploadStreamAfterRollback(event(stream));
        assertThat(stream.closed).isTrue();
        verifyNoInteractions(processor);
    }

    @Test
    void rollbackCloseFailureDoesNotInterruptOtherCallbacks() {
        var stream = new TrackingStream() {
            @Override public void close() throws IOException {
                super.close();
                throw new IOException("닫기 실패");
            }
        };
        assertThatCode(() -> listener.closeUploadStreamAfterRollback(event(stream))).doesNotThrowAnyException();
        assertThat(stream.closed).isTrue();
        verifyNoInteractions(processor);
    }

    private BinaryContentCreatedEvent event(TrackingStream stream) {
        return new BinaryContentCreatedEvent(UUID.randomUUID(), new BinaryContentUpload(stream, "text/plain", 1));
    }

    private static class TrackingStream extends ByteArrayInputStream {
        boolean closed;
        TrackingStream() { super(new byte[]{1}); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
}
