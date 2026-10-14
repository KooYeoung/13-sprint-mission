package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class UserCacheEventListener {

    @CacheEvict(cacheNames = CacheConfig.USERS, key = "#event.userId")
    @TransactionalEventListener(fallbackExecution = true)
    public void userCacheClear(UserCacheClearEvent event){
        log.debug("사용자 캐시를 무효화합니다. userId={}", event.userId());
    }
}
