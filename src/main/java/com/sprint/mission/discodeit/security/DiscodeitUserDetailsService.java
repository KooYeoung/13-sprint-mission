package com.sprint.mission.discodeit.security;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserLoginFailedException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.service.basic.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscodeitUserDetailsService implements UserDetailsService {

    private final UserReader userReader;
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userReader.getByUsername(username);
            return convertToUserDetails(user);
        } catch (UserLoginFailedException exception) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.", exception);
        }
    }

    // 폼 로그인은 사용자명으로 조회하고, 발급된 JWT의 인증과 갱신은 변경되지 않는 ID로 조회한다.
    public DiscodeitUserDetails loadUserById(UUID userId) {
        try {
            return convertToUserDetails(userReader.getUser(userId));
        } catch (UserNotFoundException exception) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.", exception);
        }
    }

    private DiscodeitUserDetails convertToUserDetails(User user) {
        return new DiscodeitUserDetails(userMapper.toDto(user, true), user.getPassword());
    }
}
