package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.service.BinaryContentStatusUpdater;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("바이너리 파일 업로드 이벤트 리스너 단위 테스트")
class BinaryContentUploadEventListenerTest {
    @Mock BinaryContentStorage storage;
    @Mock BinaryContentStatusUpdater statusUpdater;
    @InjectMocks BinaryContentUploadEventListener listener;

    @Test
    @DisplayName("저장소 업로드가 성공하면 완료 상태로 변경한다")
    void uploadAfterCommit_updatesSuccess() {
        BinaryContentCreatedEvent event = event(new TrackingInputStream());

        listener.uploadAfterCommit(event);

        verify(storage).put(event.binaryContentId(), event.contentUpload());
        verify(statusUpdater).updateStatus(event.binaryContentId(), BinaryContentStatus.SUCCESS);
        verify(statusUpdater, never()).updateStatus(event.binaryContentId(), BinaryContentStatus.FAIL);
    }

    @Test
    @DisplayName("저장소 업로드가 실패하면 실패 상태로 변경하고 같은 예외를 다시 던진다")
    void uploadAfterCommit_updatesFailAndRethrows() {
        BinaryContentCreatedEvent event = event(new TrackingInputStream());
        FileSaveFailedException failure = new FileSaveFailedException(
                event.binaryContentId(), "업로드 실패", new IOException("저장소 오류"));
        given(storage.put(event.binaryContentId(), event.contentUpload())).willThrow(failure);

        assertThatThrownBy(() -> listener.uploadAfterCommit(event)).isSameAs(failure);

        verify(statusUpdater).updateStatus(event.binaryContentId(), BinaryContentStatus.FAIL);
        verify(statusUpdater, never()).updateStatus(event.binaryContentId(), BinaryContentStatus.SUCCESS);
    }

    @Test
    @DisplayName("롤백 시 업로드와 상태 변경 없이 스트림을 닫는다")
    void closeUploadStreamAfterRollback_closesStreamOnly() {
        TrackingInputStream stream = new TrackingInputStream();

        listener.closeUploadStreamAfterRollback(event(stream));

        assertThat(stream.closed).isTrue();
        verifyNoInteractions(storage, statusUpdater);
    }

    @Test
    @DisplayName("롤백 후 스트림 정리가 실패해도 다른 후처리를 방해하지 않는다")
    void closeUploadStreamAfterRollback_handlesCloseFailure() {
        TrackingInputStream stream = new TrackingInputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                throw new IOException("스트림 정리 실패");
            }
        };

        assertThatCode(() -> listener.closeUploadStreamAfterRollback(event(stream))).doesNotThrowAnyException();

        assertThat(stream.closed).isTrue();
        verifyNoInteractions(storage, statusUpdater);
    }

    private BinaryContentCreatedEvent event(TrackingInputStream stream) {
        return new BinaryContentCreatedEvent(UUID.randomUUID(), new BinaryContentUpload(stream, "text/plain", 1));
    }

    private static class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        TrackingInputStream() {
            super(new byte[]{1});
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
