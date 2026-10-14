package com.sprint.mission.discodeit.initializer;

import java.util.Optional;

public interface AdminInitializationLock {
    /**
     * 한 번만 획득을 시도하고, 이미 잠겨 있으면 빈 값을 반환한다.
     * 획득 과정의 오류는 락 경합과 구분하여 예외로 전파한다.
     */
    Optional<String> tryAcquire();

    /**
     * 소유자 토큰이 일치하는 락만 해제한다. 초기화 트랜잭션 종료 후 호출한다.
     */
    void release(String ownerToken);
}
