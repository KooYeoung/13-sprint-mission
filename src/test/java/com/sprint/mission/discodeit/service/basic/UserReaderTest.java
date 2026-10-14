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
import java.util.Set;
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
    @Mock UserOnlineStatusReader userOnlineStatusReader;
    @InjectMocks UserReader reader;

    @Test
    void findUserDtosByIds_returnsEmptyWithoutAccessingCacheOrDatabase() {
        assertThat(reader.findUserDtosByIds(List.of())).isEmpty();
        verifyNoInteractions(repository, mapper, cacheManager, cache, userOnlineStatusReader);
    }

    @Test
    void findUserDtosByIds_skipsDatabaseWhenAllEntriesAreCached() {
        UserDto first = principal(identified(user())).getUserDto();
        UserDto second = principal(identified(user())).getUserDto();
        given(cacheManager.getCache(CacheConfig.USERS)).willReturn(cache);
        given(cache.get(first.id(), UserDto.class)).willReturn(first);
        given(cache.get(second.id(), UserDto.class)).willReturn(second);
        given(userOnlineStatusReader.findOnlineUserIds(Set.of(first.id(), second.id()))).willReturn(Set.of());

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
        given(mapper.toDto(user, false)).willReturn(loaded);
        given(userOnlineStatusReader.findOnlineUserIds(Set.of(cached.id(), loaded.id()))).willReturn(Set.of(loaded.id()));

        Map<UUID, UserDto> result = reader.findUserDtosByIds(List.of(cached.id(), loaded.id(), loaded.id(), absentId));

        assertThat(result).containsOnlyKeys(cached.id(), loaded.id()).containsEntry(cached.id(), cached);
        assertThat(result.get(loaded.id())).usingRecursiveComparison().ignoringFields("online").isEqualTo(loaded);
        assertThat(result.get(loaded.id()).online()).isTrue();
        assertThat(loaded.online()).isFalse();
        verify(repository).findByIdIn(List.of(loaded.id(), absentId));
        verifyNoMoreInteractions(repository); // 누락 ID별 단건 조회가 추가되면 안 된다.
        verify(userOnlineStatusReader).findOnlineUserIds(Set.of(cached.id(), loaded.id()));
        verifyNoMoreInteractions(userOnlineStatusReader); // 온라인 상태도 사용자마다 개별 조회하지 않는다.
        verify(cache).put(loaded.id(), loaded);
        verify(cache, never()).put(eq(absentId), any());
    }

    @Test
    void findUserDto_recalculatesOnlineWithoutReplacingCachedBaseDto() {
        UserDto cached = principal(identified(user())).getUserDto();
        given(cacheManager.getCache(CacheConfig.USERS)).willReturn(cache);
        given(cache.get(cached.id(), UserDto.class)).willReturn(cached);
        given(userOnlineStatusReader.findOnlineUserIds(Set.of(cached.id())))
                .willReturn(Set.of(cached.id())).willReturn(Set.of());

        UserDto online = reader.findUserDto(cached.id()).orElseThrow();
        UserDto offline = reader.findUserDto(cached.id()).orElseThrow();

        assertThat(online.online()).isTrue();
        assertThat(online).usingRecursiveComparison().ignoringFields("online").isEqualTo(cached);
        assertThat(offline).isEqualTo(cached);
        assertThat(cached.online()).isFalse();
        verify(cache, never()).put(any(), any());
        verifyNoInteractions(repository, mapper);
    }

    @Test
    void findUserDtosByIds_skipsOnlineLookupWhenNoUsersExist() {
        UUID absentId = UUID.randomUUID();
        given(cacheManager.getCache(CacheConfig.USERS)).willReturn(cache);
        given(repository.findByIdIn(List.of(absentId))).willReturn(List.of());

        assertThat(reader.findUserDtosByIds(List.of(absentId, absentId))).isEmpty();

        verifyNoInteractions(mapper, userOnlineStatusReader);
        verify(cache, never()).put(any(), any());
    }
}
