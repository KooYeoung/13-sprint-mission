package com.sprint.mission.discodeit.initializer;

import com.sprint.mission.discodeit.config.AdminUserProperties;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.service.AdminUserService;
import com.sprint.mission.discodeit.service.UserRoleQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 사용자 초기화 단위 테스트")
class AdminUserInitializerTest {

    @Mock
    UserRoleQueryService userRoleQueryService;

    @Mock
    AdminUserService adminUserService;

    @Mock
    ApplicationArguments applicationArguments;

    @Mock
    AdminInitializationLock initializationLock;

    private static final String OWNER_TOKEN = "test-owner-token";

    AdminUserProperties adminUserProperties;
    AdminUserInitializer adminUserInitializer;

    @BeforeEach
    void setUp() {
        adminUserProperties = new AdminUserProperties(
                "admin",
                "adminPassword",
                "admin@example.com"
        );
        adminUserInitializer = new AdminUserInitializer(
                adminUserProperties,
                userRoleQueryService,
                adminUserService,
                initializationLock
        );
    }

    @Test
    @DisplayName("관리자가 이미 존재하면 새 관리자를 생성하지 않는다")
    void run_doesNotCreateAdmin_whenAdminExists() {
        given(initializationLock.tryAcquire()).willReturn(Optional.of(OWNER_TOKEN));
        given(userRoleQueryService.existsByRole(Role.ADMIN)).willReturn(true);

        adminUserInitializer.run(applicationArguments);

        then(userRoleQueryService).should().existsByRole(Role.ADMIN);
        then(adminUserService).shouldHaveNoInteractions();
        then(initializationLock).should().release(OWNER_TOKEN);
    }

    @Test
    @DisplayName("관리자가 존재하지 않으면 설정값으로 관리자를 생성한다")
    void run_createsAdmin_whenAdminDoesNotExist() {
        given(initializationLock.tryAcquire()).willReturn(Optional.of(OWNER_TOKEN));
        UserCreateCommand expectedCommand = new UserCreateCommand(
                adminUserProperties.username(),
                adminUserProperties.password(),
                adminUserProperties.email()
        );
        given(userRoleQueryService.existsByRole(Role.ADMIN)).willReturn(false);
        given(adminUserService.createAdmin(expectedCommand)).willReturn(adminUserDto());

        adminUserInitializer.run(applicationArguments);

        then(userRoleQueryService).should().existsByRole(Role.ADMIN);
        then(adminUserService).should().createAdmin(expectedCommand);
        then(initializationLock).should().release(OWNER_TOKEN);
    }

    @Test
    @DisplayName("이미 잠겨 있으면 조회와 생성 없이 반환하고 락을 해제하지 않는다")
    void run_returnsImmediately_whenLockIsHeld() {
        given(initializationLock.tryAcquire()).willReturn(Optional.empty());

        adminUserInitializer.run(applicationArguments);

        verifyNoInteractions(userRoleQueryService, adminUserService);
        then(initializationLock).should(never()).release(any());
    }

    @Test
    @DisplayName("Redis 연결 오류는 락 경합으로 무시하지 않고 기동 실패로 전파한다")
    void run_propagatesLockAcquisitionFailure() {
        RuntimeException failure = new RedisConnectionFailureException("Redis 연결 실패");
        given(initializationLock.tryAcquire()).willThrow(failure);

        assertThatThrownBy(() -> adminUserInitializer.run(applicationArguments)).isSameAs(failure);

        verifyNoInteractions(userRoleQueryService, adminUserService);
        then(initializationLock).should(never()).release(any());
    }

    @Test
    @DisplayName("관리자 조회에 실패해도 획득한 락을 해제한다")
    void run_releasesLock_whenLookupFails() {
        RuntimeException failure = new IllegalStateException("관리자 조회 실패");
        given(initializationLock.tryAcquire()).willReturn(Optional.of(OWNER_TOKEN));
        given(userRoleQueryService.existsByRole(Role.ADMIN)).willThrow(failure);

        assertThatThrownBy(() -> adminUserInitializer.run(applicationArguments)).isSameAs(failure);

        then(initializationLock).should().release(OWNER_TOKEN);
        verifyNoInteractions(adminUserService);
    }

    @Test
    @DisplayName("관리자 생성에 실패해도 획득한 락을 해제한다")
    void run_releasesLock_whenCreationFails() {
        RuntimeException failure = new IllegalStateException("관리자 생성 실패");
        given(initializationLock.tryAcquire()).willReturn(Optional.of(OWNER_TOKEN));
        given(adminUserService.createAdmin(any())).willThrow(failure);

        assertThatThrownBy(() -> adminUserInitializer.run(applicationArguments)).isSameAs(failure);

        then(initializationLock).should().release(OWNER_TOKEN);
    }

    @Test
    @DisplayName("초기화가 정상 종료되어도 락 해제 오류는 기동 실패로 전파한다")
    void run_propagatesReleaseFailure() {
        RuntimeException failure = new RedisConnectionFailureException("락 해제 연결 실패");
        given(initializationLock.tryAcquire()).willReturn(Optional.of(OWNER_TOKEN));
        given(userRoleQueryService.existsByRole(Role.ADMIN)).willReturn(true);
        doThrow(failure).when(initializationLock).release(OWNER_TOKEN);

        assertThatThrownBy(() -> adminUserInitializer.run(applicationArguments)).isSameAs(failure);
    }

    @Test
    @DisplayName("생성과 해제가 모두 실패하면 생성 예외를 유지하고 해제 예외를 첨부한다")
    void run_preservesCreationFailure_whenReleaseAlsoFails() {
        RuntimeException creationFailure = new IllegalStateException("관리자 생성 실패");
        RuntimeException releaseFailure = new RedisConnectionFailureException("락 해제 연결 실패");
        given(initializationLock.tryAcquire()).willReturn(Optional.of(OWNER_TOKEN));
        given(adminUserService.createAdmin(any())).willThrow(creationFailure);
        doThrow(releaseFailure).when(initializationLock).release(OWNER_TOKEN);

        assertThatThrownBy(() -> adminUserInitializer.run(applicationArguments))
                .isSameAs(creationFailure).hasSuppressedException(releaseFailure);
    }

    private UserDto adminUserDto() {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-11T10:00:00+09:00");
        return new UserDto(
                UUID.randomUUID(),
                adminUserProperties.username(),
                adminUserProperties.email(),
                null,
                false,
                Role.ADMIN,
                now,
                now
        );
    }
}
