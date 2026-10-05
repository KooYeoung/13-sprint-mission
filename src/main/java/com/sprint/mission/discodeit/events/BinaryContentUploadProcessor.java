package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.service.BinaryContentStatusUpdater;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinaryContentUploadProcessor {
    private final BinaryContentStorage storage;
    private final BinaryContentStatusUpdater statusUpdater;

    @Async("fileUploadExecutor")
    public void upload(BinaryContentCreatedEvent event) {
        log.info("커밋된 바이너리 파일 업로드를 시작합니다. binaryContentId={}", event.binaryContentId());
        try {
            // 저장소는 성공·실패 모두 전달받은 스트림을 닫는다.
            storage.put(event.binaryContentId(), event.contentUpload());
        } catch (RuntimeException failure) {
            log.error("바이너리 파일 업로드에 실패했습니다. binaryContentId={}", event.binaryContentId(), failure);
            recordFailure(event.binaryContentId(), failure);
            throw failure;
        }
        try {
            statusUpdater.updateStatus(event.binaryContentId(), BinaryContentStatus.SUCCESS);
            log.info("바이너리 파일 업로드와 상태 저장을 완료했습니다. binaryContentId={}", event.binaryContentId());
        } catch (RuntimeException failure) {
            log.error("파일 업로드 후 완료 상태 저장에 실패했습니다. binaryContentId={}", event.binaryContentId(), failure);
            throw failure;
        }
    }

    public void recordFailure(UUID id, RuntimeException cause) {
        // 인터럽트된 작업도 별도 트랜잭션으로 실패 상태를 기록한 후 플래그를 복원한다.
        boolean interrupted = Thread.interrupted();
        try {
            statusUpdater.updateStatus(id, BinaryContentStatus.FAIL);
        } catch (RuntimeException statusFailure) {
            if (cause != statusFailure) cause.addSuppressed(statusFailure);
            log.error("업로드 실패 상태 저장에 실패했습니다. binaryContentId={}", id, statusFailure);
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
