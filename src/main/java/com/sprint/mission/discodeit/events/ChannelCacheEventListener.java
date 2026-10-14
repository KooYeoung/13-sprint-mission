package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class ChannelCacheEventListener {

    @CacheEvict(cacheNames = CacheConfig.CHANNELS, key = "#event.channelId")
    @TransactionalEventListener
    public void channelClearEvent(ChannelCacheClearEvent event){
        log.debug("채널 캐시를 무효화합니다. channelId={}", event.channelId());
    }
}
