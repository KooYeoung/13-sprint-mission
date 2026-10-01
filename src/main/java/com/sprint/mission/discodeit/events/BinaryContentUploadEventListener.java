package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.exception.storage.FileSaveFailedException;
import com.sprint.mission.discodeit.service.BinaryContentStatusUpdater;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinaryContentUploadEventListener {

    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentStatusUpdater binaryContentStatusUpdater;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void uploadAfterCommit(BinaryContentCreatedEvent event) {
        log.info("메타데이터 커밋 후 바이너리 파일 업로드를 시작합니다. binaryContentId={}", event.binaryContentId());
        try {
            binaryContentStorage.put(event.binaryContentId(), event.contentUpload());
            log.info("바이너리 파일 업로드를 완료했습니다. binaryContentId={}", event.binaryContentId());
            binaryContentStatusUpdater.updateStatus(event.binaryContentId(), BinaryContentStatus.SUCCESS);
        } catch (FileSaveFailedException e) {
            log.error("바이너리 파일 업로드에 실패했습니다. binaryContentId={}, cause={}",
                    event.binaryContentId(), e.getMessage());
            binaryContentStatusUpdater.updateStatus(event.binaryContentId(), BinaryContentStatus.FAIL);
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void closeUploadStreamAfterRollback(BinaryContentCreatedEvent event) {
        try {
            event.contentUpload().inputStream().close();
            log.debug("트랜잭션 롤백으로 업로드 스트림을 닫았습니다. binaryContentId={}", event.binaryContentId());
        } catch (IOException e) {
            log.warn(
                    "트랜잭션 롤백 후 업로드 스트림 정리에 실패했습니다. binaryContentId={}",
                    event.binaryContentId(),
                    e
            );
        }
    }
}
