package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class NotificationCacheClearEventListener {

    @CacheEvict(cacheNames = CacheConfig.USERS_BY_NOTIFICATIONS, key = "#event.userId")
    @TransactionalEventListener
    public void notificationClearEvent(NotificationCacheClearEvent event){
        log.debug("사용자의 알림 캐시를 무효화합니다. userId={}", event.userId());
    }
}
