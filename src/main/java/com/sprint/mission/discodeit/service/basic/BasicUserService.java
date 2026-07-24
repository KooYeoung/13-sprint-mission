package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.aspect.LogAction;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserEmailDuplicatedException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserUsernameDuplicatedException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
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
    private final ReadStatusService readStatusService;
    private final MessageService messageService;
    private final UserMapper userMapper;

    @LogAction(value = "사용자 생성")
    @Override
    public UserDto create(UserCreateCommand command, MultipartFile file) {
        validateCreatableUser(command);

        BinaryContent profile = binaryContentService.create(file).orElse(null);

        User savedUser = userRepository.save(new User(command, profile));

        UserStatusDto userStatusDto = userStatusService.create(savedUser, new UserStatusCreateCommand(Instant.now()));

        return userMapper.toDto(savedUser, userStatusDto.isOnline());
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto findById(UUID userId) {
        User user = getUserRequireThrow(userId);

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @LogAction(value = "사용자 수정")
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

        return userMapper.toDto(updatedUser);
    }

    @LogAction(value = "사용자 삭제", idName = "userId", idParamIndex = 0)
    @Override
    public void delete(UUID userId) {
        User user = getUserRequireThrow(userId);

        userStatusService.delete(user.getStatusId(), userId);
        readStatusService.deleteByUserId(userId);
        messageService.detachByAuthorId(userId);

        userRepository.deleteById(user.getId());

        if (user.isProfileImageExist()) {
            binaryContentService.delete(user.getProfile());
        }
    }

    private void validateCreatableUser(UserCreateCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserEmailDuplicatedException(command.email());
        }
        if (userRepository.existsByUsername(command.username())) {
            throw new UserUsernameDuplicatedException(command.username());
        }
    }

    private User getUserRequireThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void checkUserUpdates(UserUpdateCommand command, User user) {
        if (user.hasUsername(command.username()) && user.hasEmail(command.email())) {
            return;
        }

        if (!user.hasEmail(command.email()) && userRepository.existsByEmail(command.email())) {
            throw new UserEmailDuplicatedException(command.email());
        }

        if (!user.hasUsername(command.username()) && userRepository.existsByUsername(command.username())) {
            throw new UserUsernameDuplicatedException(command.username());
        }
    }

}
