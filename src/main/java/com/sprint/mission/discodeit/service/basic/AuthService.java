package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.user.UserLoginCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserStatusService userStatusService;
    private final UserReader userReader;
    private final UserMapper userMapper;

    public UserDto login(UserLoginCommand command) {
        User user = userReader.getUserByCredentials(command.username(), command.password());

        Instant now = Instant.now();

        UserStatusDto userStatusDto = userStatusService.updateByUserId(user.getId(), new UserStatusUpdateCommand(now));

        return userMapper.toDto(user, userStatusDto.isOnline());
    }
}
