package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.service.TransactionalStorageAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
public class StorageTransactionManager {

    public void afterCommit(String description, TransactionalStorageAction action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.debug("트랜잭션 동기화가 활성화되어 있지 않아 커밋 후 스토리지 작업을 즉시 실행합니다. description={}", description);
            executeQuietly(description, action);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.debug("커밋 후 스토리지 작업을 실행합니다. description={}", description);
                executeQuietly(description, action);
            }
        });

        log.debug("커밋 후 스토리지 작업을 등록했습니다. description={}", description);
    }

    public void afterRollback(String description, TransactionalStorageAction action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("트랜잭션 동기화가 활성화되어 있지 않아 롤백 후 스토리지 작업을 등록할 수 없습니다. description={}", description);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    log.debug("롤백 후 스토리지 작업을 실행합니다. description={}", description);
                    executeQuietly(description, action);
                }
            }
        });

        log.debug("롤백 후 스토리지 작업을 등록했습니다. description={}", description);
    }

    private void executeQuietly(String description, TransactionalStorageAction action) {
        try {
            action.execute();
            log.debug("트랜잭션 스토리지 작업을 완료했습니다. description={}", description);
        } catch (Exception e) {
            log.error("트랜잭션 스토리지 작업에 실패했습니다. description={}", description, e);
        }
    }
}
