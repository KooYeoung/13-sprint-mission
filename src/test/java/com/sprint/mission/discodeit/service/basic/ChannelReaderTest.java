package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.config.CacheConfig;
import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelReaderTest {
    @Mock ChannelRepository repository;
    @Mock CacheManager cacheManager;
    @Mock Cache cache;
    @InjectMocks ChannelReader reader;

    @Test
    void findChannelSummariesByIds_returnsEmptyWithoutAccessingCacheOrDatabase() {
        assertThat(reader.findChannelSummariesByIds(List.of())).isEmpty();
        verifyNoInteractions(repository, cacheManager, cache);
    }

    @Test
    void findChannelSummariesByIds_skipsDatabaseWhenAllEntriesAreCached() {
        ChannelSummary first = summary();
        ChannelSummary second = summary();
        given(cacheManager.getCache(CacheConfig.CHANNELS)).willReturn(cache);
        given(cache.get(first.id(), ChannelSummary.class)).willReturn(first);
        given(cache.get(second.id(), ChannelSummary.class)).willReturn(second);

        assertThat(reader.findChannelSummariesByIds(List.of(first.id(), second.id())))
                .containsExactlyInAnyOrderEntriesOf(Map.of(first.id(), first, second.id(), second));
        verifyNoInteractions(repository);
    }

    @Test
    void findChannelSummariesByIds_loadsDistinctMissesTogetherAndDoesNotCacheAbsentChannels() {
        ChannelSummary cached = summary();
        ChannelSummary loaded = summary();
        UUID absentId = UUID.randomUUID();
        given(cacheManager.getCache(CacheConfig.CHANNELS)).willReturn(cache);
        given(cache.get(cached.id(), ChannelSummary.class)).willReturn(cached);
        given(repository.findSummariesByIdIn(List.of(loaded.id(), absentId))).willReturn(List.of(loaded));

        assertThat(reader.findChannelSummariesByIds(List.of(cached.id(), loaded.id(), loaded.id(), absentId)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(cached.id(), cached, loaded.id(), loaded));
        verify(repository).findSummariesByIdIn(List.of(loaded.id(), absentId));
        verifyNoMoreInteractions(repository); // 누락 ID별 단건 조회가 추가되면 안 된다.
        verify(cache).put(loaded.id(), loaded);
        verify(cache, never()).put(eq(absentId), any());
    }

    private ChannelSummary summary() {
        return new ChannelSummary(UUID.randomUUID(), ChannelType.PUBLIC, "channel", "description", null);
    }
}
