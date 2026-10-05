package com.sprint.mission.discodeit.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class BinaryContentUploadEventListener {

    private final BinaryContentUploadProcessor processor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void uploadAfterCommit(BinaryContentCreatedEvent event) {
        try {
            processor.upload(event);
        } catch (TaskRejectedException rejection) {
            log.error("실행기가 업로드 작업을 거부했습니다. binaryContentId={}", event.binaryContentId(), rejection);
            deleteTemporaryUpload(event);
            processor.recordFailure(event.binaryContentId(), rejection);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void deleteTemporaryUploadAfterRollback(BinaryContentCreatedEvent event) {
        deleteTemporaryUpload(event);
    }

    private void deleteTemporaryUpload(BinaryContentCreatedEvent event) {
        try {
            event.contentUpload().close();
            log.debug("실행하지 않는 업로드의 임시 파일을 삭제했습니다. binaryContentId={}", event.binaryContentId());
        } catch (IOException | RuntimeException e) {
            log.warn(
                    "실행하지 않는 업로드의 임시 파일 삭제에 실패했습니다. binaryContentId={}",
                    event.binaryContentId(),
                    e
            );
        }
    }
}
