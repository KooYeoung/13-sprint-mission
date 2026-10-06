package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.config.CacheConfig;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
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

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReaderTest {
    @Mock UserRepository repository;
    @Mock UserMapper mapper;
    @Mock CacheManager cacheManager;
    @Mock Cache cache;
    @InjectMocks UserReader reader;

    @Test
    void findUserDtosByIds_returnsEmptyWithoutAccessingCacheOrDatabase() {
        assertThat(reader.findUserDtosByIds(List.of())).isEmpty();
        verifyNoInteractions(repository, mapper, cacheManager, cache);
    }

    @Test
    void findUserDtosByIds_skipsDatabaseWhenAllEntriesAreCached() {
        UserDto first = principal(identified(user())).getUserDto();
        UserDto second = principal(identified(user())).getUserDto();
        given(cacheManager.getCache(CacheConfig.USERS)).willReturn(cache);
        given(cache.get(first.id(), UserDto.class)).willReturn(first);
        given(cache.get(second.id(), UserDto.class)).willReturn(second);

        assertThat(reader.findUserDtosByIds(List.of(first.id(), second.id())))
                .containsExactlyInAnyOrderEntriesOf(Map.of(first.id(), first, second.id(), second));
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void findUserDtosByIds_loadsDistinctMissesTogetherAndDoesNotCacheAbsentUsers() {
        UserDto cached = principal(identified(user())).getUserDto();
        User user = identified(user());
        UserDto loaded = principal(user).getUserDto();
        UUID absentId = UUID.randomUUID();
        given(cacheManager.getCache(CacheConfig.USERS)).willReturn(cache);
        given(cache.get(cached.id(), UserDto.class)).willReturn(cached);
        given(repository.findByIdIn(List.of(loaded.id(), absentId))).willReturn(List.of(user));
        given(mapper.toDto(user)).willReturn(loaded);

        assertThat(reader.findUserDtosByIds(List.of(cached.id(), loaded.id(), loaded.id(), absentId)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(cached.id(), cached, loaded.id(), loaded));
        verify(repository).findByIdIn(List.of(loaded.id(), absentId));
        verifyNoMoreInteractions(repository); // 누락 ID별 단건 조회가 추가되면 안 된다.
        verify(cache).put(loaded.id(), loaded);
        verify(cache, never()).put(eq(absentId), any());
    }
}
