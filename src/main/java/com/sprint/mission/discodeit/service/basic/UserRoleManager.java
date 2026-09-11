package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.user.UserRoleUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRoleManager {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto updateRole(UUID userId, UserRoleUpdateCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.updateRole(command);

        return userMapper.toDto(user);
    }
}
