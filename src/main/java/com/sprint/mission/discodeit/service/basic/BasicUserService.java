package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserBadRequestException;
import com.sprint.mission.discodeit.exception.user.UserError;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BasicUserService implements UserService {
    private final UserRepository userRepository;
    private final BinaryContentService binaryContentService;
    private final UserStatusService userStatusService;

    @Override
    public UserDto create(UserCreateCommand command, MultipartFile file) {

        existThrow(userRepository.existsByEmail(command.email()), UserError.EMAIL.getMessage());
        existThrow(userRepository.existsByUsername(command.username()), UserError.USERNAME.getMessage());

        BinaryContent profile = binaryContentService.create(file).orElse(null);

        User savedUser = userRepository.save(new User(command, profile));

        UserStatusDto userStatusDto = userStatusService.create(savedUser, new UserStatusCreateCommand(Instant.now()));

        return UserDto.from(savedUser).withOnline(userStatusDto.isOnline());
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto findById(UUID userId) {
        User user = getUserRequireThrow(userId);

        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> findAll() {

        return userRepository.findAll()
                .stream()
                .map(UserDto::from)
                .toList();
    }

    @Override
    public UserDto update(UUID userId, UserUpdateCommand command, MultipartFile file) {
        User user = getUserRequireThrow(userId);

        checkUserUpdates(command, user);

        BinaryContent oldImage = user.getProfile();
        BinaryContent newImage = binaryContentService.create(file).orElse(oldImage);

        user.updateInfo(command, newImage);

        User updatedUser = userRepository.save(user);

        if (oldImage != null && !oldImage.getId().equals(newImage.getId())) {
            binaryContentService.delete(oldImage);
        }

        return UserDto.from(updatedUser);
    }

    @Override
    public void delete(UUID userId) {
        User user = getUserRequireThrow(userId);

        userRepository.deleteById(user.getId());

        if (user.isProfileImageExist()) {
            binaryContentService.delete(user.getProfile());
        }

    }

    private void existThrow(boolean exist, String message) {
        if (exist) throw new UserBadRequestException(message);
    }

    private User getUserRequireThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private void checkUserUpdates(UserUpdateCommand command, User user) {
        if (user.hasUsername(command.username()) && user.hasEmail(command.email())) {
            return;
        }
        // 이메일 검증.
        if (!user.hasEmail(command.email())) {
            existThrow(userRepository.existsByEmail(command.email()), UserError.EMAIL.getMessage());
        }

        if (!user.hasUsername(command.username())) {
            existThrow(userRepository.existsByUsername(command.username()), UserError.USERNAME.getMessage());
        }
    }

}
