package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.userStatus.UserStatusBadRequestException;
import com.sprint.mission.discodeit.exception.userStatus.UserStatusNotFoundException;
import com.sprint.mission.discodeit.mapper.UserStatusMapper;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserStatusService {
    private final UserStatusRepository userStatusRepository;
    private final UserReader userReader;
    private final UserStatusMapper userStatusMapper;

    public UserStatusDto create(UUID userId, UserStatusCreateCommand command) {

        if (userStatusRepository.existsByUser_Id(userId)) {
            throw new UserStatusBadRequestException(ErrorCode.USER_STATUS_ALREADY_EXISTS, userId);
        }

        User user = getUserRequireThrow(userId);

        return userStatusMapper.toDto(userStatusRepository.save(new UserStatus(user, command)));
    }

    public UserStatusDto create(User user, UserStatusCreateCommand command) {

        if (userStatusRepository.existsByUser_Id(user.getId())) {
            throw new UserStatusBadRequestException(ErrorCode.USER_STATUS_ALREADY_EXISTS, user.getId());
        }

        return userStatusMapper.toDto(userStatusRepository.save(new UserStatus(user, command)));
    }

    @Transactional(readOnly = true)
    public UserStatusDto findById(UUID userStatusId, UUID userId) {
        UserStatus userStatus = getUserStatusRequireThrow(userStatusId, userId);

        return userStatusMapper.toDto(userStatus);
    }

    public UserStatusDto update(UUID userStatusId, UUID userId, UserStatusUpdateCommand command) {
        Optional<UserStatus> statusOptional = userStatusRepository.findByIdAndUser_Id(userStatusId, userId);

        UserStatus status = createOrUpdateUserStatus(userId, command, statusOptional);

        return userStatusMapper.toDto(userStatusRepository.save(status));
    }

    public UserStatusDto updateByUserId(UUID userId, UserStatusUpdateCommand command) {
        Optional<UserStatus> userStatusResult = userStatusRepository.findByUser_Id(userId);

        UserStatus status = createOrUpdateUserStatus(userId, command, userStatusResult);

        return userStatusMapper.toDto(userStatusRepository.save(status));
    }

    public void delete(UUID userStatusId, UUID userId) {
        if (!userStatusRepository.existsByIdAndUser_Id(userStatusId, userId)) return;
        userStatusRepository.deleteById(userStatusId);
    }

    private UserStatus getUserStatusRequireThrow(UUID userStatusId, UUID userId) {
        return userStatusRepository.findByIdAndUser_Id(userStatusId, userId)
                .orElseThrow(() -> new UserStatusNotFoundException(userStatusId));
    }

    private User getUserRequireThrow(UUID userId) {
        return userReader.getUser(userId);
    }

    private @NonNull UserStatus createOrUpdateUserStatus(UUID userId, UserStatusUpdateCommand command, Optional<UserStatus> statusOptional) {
        if (statusOptional.isPresent()) {
            UserStatus currentStatus = statusOptional.get();
            currentStatus.updateInfo(command);
            return currentStatus;
        }

        return new UserStatus(
                getUserRequireThrow(userId),
                new UserStatusCreateCommand(command.updateAt())
        );
    }

}
