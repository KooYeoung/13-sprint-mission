package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.user.UserLoginCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.LoginFailException;
import com.sprint.mission.discodeit.exception.user.UserError;
import com.sprint.mission.discodeit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserStatusService userStatusService;

    public UserDto login(UserLoginCommand command) {
        User user = userRepository.findByUsernameAndPassword(command.username(), command.password())
                .orElseThrow(() -> new LoginFailException(UserError.LOGIN.getMessage()));

        Instant now = Instant.now();

        UserStatusDto userStatusDto = userStatusService.updateByUserId(user.getId(), new UserStatusUpdateCommand(now));

        return UserDto.from(user).withOnline(userStatusDto.online());
    }
}
