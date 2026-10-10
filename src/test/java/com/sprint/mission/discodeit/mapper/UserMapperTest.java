package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserRoleUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.security.JwtRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UserMapperImpl.class, BinaryContentMapperImpl.class, UserOnlineMapper.class})
@DisplayName("UserMapper 단위 테스트")
class UserMapperTest {

    @Autowired
    UserMapper userMapper;

    @MockitoBean
    JwtRegistry jwtRegistry;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("명시한 온라인 상태와 사용자 역할을 별도 레지스트리 조회 없이 매핑한다")
    void toDto_mapsUserRoleAndExplicitOnlineState(boolean online) {
        User user = new User(
                new UserCreateCommand("testUser", "encodedPassword", "test@example.com"),
                null
        );
        user.updateRole(new UserRoleUpdateCommand(Role.CHANNEL_MANAGER));

        UserDto result = userMapper.toDto(user, online);

        assertThat(result.username()).isEqualTo(user.getUsername());
        assertThat(result.email()).isEqualTo(user.getEmail());
        assertThat(result.online()).isEqualTo(online);
        assertThat(result.role()).isEqualTo(Role.CHANNEL_MANAGER);
        verifyNoInteractions(jwtRegistry);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("실제 매퍼가 레지스트리의 사용자 온라인 상태를 DTO에 반영한다")
    void toDto_mapsRegistryOnlineState(boolean active) {
        User user = new User(new UserCreateCommand("testUser", "encodedPassword", "test@example.com"), null);
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(user, "id", userId);
        given(jwtRegistry.hasActiveJwtInformationByUserId(userId)).willReturn(active);

        UserDto result = userMapper.toDto(user);

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.username()).isEqualTo(user.getUsername());
        assertThat(result.online()).isEqualTo(active);
    }
}
