package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.user.UserRoleUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.events.RoleUpdatedEvent;
import com.sprint.mission.discodeit.events.UserCacheClearEvent;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.security.JwtRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRoleManager {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtRegistry jwtRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserDto updateRole(UUID userId, UserRoleUpdateCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        publishRoleUpdateEvent(command, user);
        user.updateRole(command);
        jwtRegistry.invalidateJwtInformationByUserId(userId);
        eventPublisher.publishEvent(new UserCacheClearEvent(userId));
        return userMapper.toDto(user);
    }

    private void publishRoleUpdateEvent(UserRoleUpdateCommand command, User user) {
        if (user.getRole().equals(command.newRole())) {
            log.debug("권한 변경이 없어 알림 이벤트 발행을 생략합니다. userId={}, role={}", user.getId(), user.getRole());
            return;
        }
        eventPublisher.publishEvent(new RoleUpdatedEvent(user.getId(), user.getRole(), command.newRole()));
        log.debug("권한 변경 이벤트를 발행했습니다. userId={}, previousRole={}, newRole={}",
                user.getId(), user.getRole(), command.newRole());

    }
}
