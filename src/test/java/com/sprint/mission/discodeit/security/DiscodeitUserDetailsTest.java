package com.sprint.mission.discodeit.security;

import com.sprint.mission.discodeit.dto.response.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DiscodeitUserDetails 단위 테스트")
class DiscodeitUserDetailsTest {

    @Test
    @DisplayName("사용자 DTO와 비밀번호를 Spring Security 인증 정보로 제공한다")
    void providesUserDetails() {
        UserDto userDto = userDto("testUser");
        String password = "$2a$10$encodedPassword";

        DiscodeitUserDetails userDetails = new DiscodeitUserDetails(userDto, password);

        assertThat(userDetails.getUserDto()).isSameAs(userDto);
        assertThat(userDetails.getUsername()).isEqualTo(userDto.username());
        assertThat(userDetails.getPassword()).isEqualTo(password);
        assertThat(userDetails.getAuthorities()).isEmpty();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    private UserDto userDto(String username) {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-10T10:00:00+09:00");
        return new UserDto(
                UUID.randomUUID(),
                username,
                "test@example.com",
                null,
                true,
                now,
                now
        );
    }
}
