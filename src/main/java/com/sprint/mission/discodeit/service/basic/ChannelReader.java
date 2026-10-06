package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.config.CacheConfig;
import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelReader {

    private final ChannelRepository channelRepository;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = CacheConfig.CHANNELS, key = "#channelId", unless = "#result == null")
    public Optional<ChannelSummary> findChannelSummary(UUID channelId) {
        return channelRepository.findByDetail(channelId);
    }

    public Map<UUID, ChannelSummary> findChannelSummariesByIds(Collection<UUID> channelIds) {
        if (channelIds.isEmpty()) return Map.of();

        Cache cache = Objects.requireNonNull(cacheManager.getCache(CacheConfig.CHANNELS), "채널 캐시가 설정되지 않았습니다.");
        Map<UUID, ChannelSummary> summaries = new HashMap<>();
        List<UUID> missingIds = new ArrayList<>();
        for (UUID channelId : new LinkedHashSet<>(channelIds)) {
            ChannelSummary cached = cache.get(channelId, ChannelSummary.class);
            if (cached == null) missingIds.add(channelId);
            else summaries.put(channelId, cached);
        }

        if (!missingIds.isEmpty()) {
            for (ChannelSummary summary : channelRepository.findSummariesByIdIn(missingIds)) {
                cache.put(summary.id(), summary);
                summaries.put(summary.id(), summary);
            }
        }
        return summaries;
    }

    public Channel getChannel(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));
    }

    public boolean isChannelAccessible(UUID channelId, UUID userId) {
        return channelRepository.isChannelAccessible(channelId, userId);
    }
}
