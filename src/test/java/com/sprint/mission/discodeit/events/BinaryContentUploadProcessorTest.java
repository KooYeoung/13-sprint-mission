package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.service.BinaryContentStatusUpdater;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("바이너리 파일 업로드 처리 단위 테스트")
class BinaryContentUploadProcessorTest {
    @TempDir Path temporaryDirectory;
    @Mock BinaryContentStorage storage;
    @Mock BinaryContentStatusUpdater statusUpdater;
    private BinaryContentUploadProcessor processor;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        processor = new BinaryContentUploadProcessor(storage, statusUpdater);
    }

    @Test
    @DisplayName("저장소 업로드가 성공하면 완료 상태로 변경한다")
    void uploadAfterCommit_updatesSuccess() throws IOException {
        BinaryContentCreatedEvent event = event();

        processor.upload(event);

        verify(storage).put(event.binaryContentId(), event.contentUpload());
        verify(statusUpdater).updateStatus(event.binaryContentId(), BinaryContentStatus.SUCCESS);
        verify(statusUpdater, never()).updateStatus(event.binaryContentId(), BinaryContentStatus.FAIL);
        assertThat(event.contentUpload().temporaryFile()).doesNotExist();
    }

    @Test
    @DisplayName("저장소 업로드가 실패하면 실패 상태로 변경하고 같은 예외를 다시 던진다")
    void uploadAfterCommit_updatesFailAndRethrows() throws IOException {
        BinaryContentCreatedEvent event = event();
        FileSaveFailedException failure = new FileSaveFailedException(
                event.binaryContentId(), "업로드 실패", new IOException("저장소 오류"));
        given(storage.put(event.binaryContentId(), event.contentUpload())).willThrow(failure);

        assertThatThrownBy(() -> processor.upload(event)).isSameAs(failure);

        verify(statusUpdater).updateStatus(event.binaryContentId(), BinaryContentStatus.FAIL);
        verify(statusUpdater, never()).updateStatus(event.binaryContentId(), BinaryContentStatus.SUCCESS);
        assertThat(event.contentUpload().temporaryFile()).doesNotExist();
    }

    @Test
    void statusFailureAfterSuccessfulStorageDoesNotMarkUploadAsFailed() throws IOException {
        var event = event();
        var failure = new IllegalStateException("상태 저장 실패");
        doThrow(failure).when(statusUpdater).updateStatus(event.binaryContentId(), BinaryContentStatus.SUCCESS);
        assertThatThrownBy(() -> processor.upload(event)).isSameAs(failure);
        verify(statusUpdater, never()).updateStatus(event.binaryContentId(), BinaryContentStatus.FAIL);
        assertThat(event.contentUpload().temporaryFile()).doesNotExist();
    }

    @Test
    void preservesOriginalStorageFailureWhenFailStatusCannotBeSaved() throws IOException {
        var event = event();
        var failure = new IllegalStateException("저장소 오류");
        var statusFailure = new IllegalStateException("상태 저장 오류");
        given(storage.put(event.binaryContentId(), event.contentUpload())).willThrow(failure);
        doThrow(statusFailure).when(statusUpdater).updateStatus(event.binaryContentId(), BinaryContentStatus.FAIL);
        assertThatThrownBy(() -> processor.upload(event)).isSameAs(failure);
        assertThat(failure.getSuppressed()).containsExactly(statusFailure);
        assertThat(event.contentUpload().temporaryFile()).doesNotExist();
    }

    private BinaryContentCreatedEvent event() throws IOException {
        Path file = Files.write(temporaryDirectory.resolve(UUID.randomUUID().toString()), new byte[]{1});
        return new BinaryContentCreatedEvent(UUID.randomUUID(), new BinaryContentUpload(file, "text/plain", 1));
    }
}
