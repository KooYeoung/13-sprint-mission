package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.service.basic.UserOnlineStatusReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 온라인 상태 매퍼 단위 테스트")
class UserOnlineMapperTest {

    @InjectMocks
    UserOnlineMapper userOnlineMapper;

    @Mock
    UserOnlineStatusReader userOnlineStatusReader;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("해당 사용자 ID의 온라인 상태 판단을 전담 리더에 위임한다")
    void isOnline_returnsReaderStateForRequestedUser(boolean active) {
        UUID userId = UUID.randomUUID();
        given(userOnlineStatusReader.isOnline(userId)).willReturn(active);

        assertThat(userOnlineMapper.isOnline(userId)).isEqualTo(active);

        verify(userOnlineStatusReader).isOnline(userId);
    }
}
