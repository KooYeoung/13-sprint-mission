package com.sprint.mission.discodeit.service.basic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class FileTransactionManager {

    /**
     * 새 파일 저장 후 DB 트랜잭션이 롤백되면 실제 파일을 삭제한다.
     */
    public void deleteOnRollback(Path path) {
        if (path == null) return;

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("트랜잭션 동기화가 활성화되어 있지 않아 rollback 파일 정리를 예약할 수 없습니다. path={}", path);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteQuietly(path);
                }
            }
        });

    }

    /**
     * DB 트랜잭션이 커밋된 이후 실제 파일을 삭제한다.
     */
    public void deleteAfterCommit(Path path) {
        if (path == null) return;

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(path);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(path);
            }
        });

    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
            log.info("파일 삭제 완료. path={}", path);
        } catch (IOException e) {
            log.error("파일 삭제 중 오류가 발생했습니다. path={}", path, e);
        }

    }
}
