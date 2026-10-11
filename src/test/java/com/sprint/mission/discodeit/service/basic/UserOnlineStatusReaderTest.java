package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.security.JwtRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 온라인 표시 기준 단위 테스트")
class UserOnlineStatusReaderTest {

    @Mock JwtRegistry jwtRegistry;
    @InjectMocks UserOnlineStatusReader reader;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("유효한 Access Token이 있는 사용자만 온라인으로 표시한다")
    void isOnline_usesRegisteredValidAccessToken(boolean expected) {
        UUID userId = UUID.randomUUID();
        given(jwtRegistry.findUserIdsWithValidAccessToken(List.of(userId)))
                .willReturn(expected ? Set.of(userId) : Set.of());

        assertThat(reader.isOnline(userId)).isEqualTo(expected);
    }

    @Test
    @DisplayName("일괄 조회는 레지스트리의 유효한 Access Token 보유 사용자 목록을 반환한다")
    void findOnlineUserIds_usesOneBatchForAllUsers() {
        UUID active = UUID.randomUUID();
        UUID expired = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        List<UUID> ids = List.of(active, expired, missing, active);
        given(jwtRegistry.findUserIdsWithValidAccessToken(ids)).willReturn(Set.of(active));

        assertThat(reader.findOnlineUserIds(ids)).containsExactly(active);

        verify(jwtRegistry).findUserIdsWithValidAccessToken(ids);
        verifyNoMoreInteractions(jwtRegistry); // 사용자별 단건 조회가 추가되지 않도록 검증한다.
    }

    @Test
    @DisplayName("빈 사용자 목록은 레지스트리를 조회하지 않는다")
    void findOnlineUserIds_skipsEmptyInput() {
        assertThat(reader.findOnlineUserIds(List.of())).isEmpty();
        verifyNoInteractions(jwtRegistry);
    }
}
