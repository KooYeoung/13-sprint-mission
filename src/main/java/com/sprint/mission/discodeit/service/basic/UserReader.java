package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserLoginFailedException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReader {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isUserExist(UUID userId) {
        return userRepository.existsById(userId);
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFoundException(userId));
    }

    public User getUserByCredentials(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserLoginFailedException::new);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserLoginFailedException();
        }

        return user;
    }
}
