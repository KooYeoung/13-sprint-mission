package com.sprint.mission.discodeit.initializer;

import com.sprint.mission.discodeit.config.AdminUserProperties;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.service.AdminUserService;
import com.sprint.mission.discodeit.service.UserRoleQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements ApplicationRunner {

    private final AdminUserProperties adminUserProperties;
    private final UserRoleQueryService userRoleQueryService;
    private final AdminUserService adminUserService;
    private final AdminInitializationLock initializationLock;

    @Override
    public void run(ApplicationArguments args) {
        Optional<String> ownerToken = initializationLock.tryAcquire();
        if (ownerToken.isEmpty()) {
            log.info("관리자 초기화가 이미 진행 중이므로 초기화를 생략합니다.");
            return;
        }

        Throwable initializationFailure = null;
        try {
            // 락 안에서 존재 여부를 확인하여 같은 락을 사용하는 초기화가 겹치지 않도록 한다.
            if (userRoleQueryService.existsByRole(Role.ADMIN)) {
                log.info("관리자 계정이 이미 존재합니다.");
                return;
            }

            UserDto adminUser = adminUserService.createAdmin(createAdminUserCommand());
            log.info("관리자 계정을 생성했습니다. userId={}", adminUser.id());
        } catch (RuntimeException | Error e) {
            initializationFailure = e;
            log.error("관리자 계정 초기화에 실패했습니다.", e);
            throw e;
        } finally {
            // createAdmin의 트랜잭션 프록시가 커밋 또는 롤백을 마친 뒤 해제한다.
            try {
                initializationLock.release(ownerToken.get());
            } catch (RuntimeException | Error releaseFailure) {
                if (initializationFailure == null) {
                    throw releaseFailure;
                }
                if (initializationFailure != releaseFailure) {
                    initializationFailure.addSuppressed(releaseFailure);
                }
            }
        }
    }

    private UserCreateCommand createAdminUserCommand() {
        return new UserCreateCommand(
                adminUserProperties.username(),
                adminUserProperties.password(),
                adminUserProperties.email()
        );
    }
}
