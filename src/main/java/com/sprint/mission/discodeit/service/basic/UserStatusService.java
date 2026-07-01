package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.exception.userStatus.UserStatusBadRequestException;
import com.sprint.mission.discodeit.exception.userStatus.UserStatusNotFoundException;
import com.sprint.mission.discodeit.repository.UserRepository;
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
    private final UserRepository userRepository;

    public UserStatusDto create(UUID userId, UserStatusCreateCommand command) {

        if (userStatusRepository.existsByUser_Id(userId)) throw new UserStatusBadRequestException("이미 유저 상태가 존재합니다.");

        User user = getUserRequireThrow(userId);

        return createUserStatus(command, user);
    }

    public UserStatusDto create(User user, UserStatusCreateCommand command) {

        if (userStatusRepository.existsByUser_Id(user.getId())) throw new UserStatusBadRequestException("이미 유저 상태가 존재합니다.");

        return createUserStatus(command, user);
    }

    @Transactional(readOnly = true)
    public UserStatusDto findById(UUID userStatusId, UUID userId) {
        UserStatus userStatus = getUserStatusRequireThrow(userStatusId, userId);

        return UserStatusDto.from(userStatus);
    }

    public UserStatusDto update(UUID userStatusId, UUID userId, UserStatusUpdateCommand command) {
        Optional<UserStatus> statusOptional = userStatusRepository.findByIdAndUser_Id(userStatusId, userId);
        if (statusOptional.isPresent()) {
            UserStatus currentStatus = statusOptional.get();
            currentStatus.updateInfo(command);
            return UserStatusDto.from(userStatusRepository.save(currentStatus));
        }

        return create(userId, new UserStatusCreateCommand(command.updateAt()));
    }

    public UserStatusDto updateByUserId(UUID userId, UserStatusUpdateCommand command) {
        Optional<UserStatus> userStatusResult = userStatusRepository.findByUser_Id(userId);

        if (userStatusResult.isPresent()) {
            UserStatus userStatus = userStatusResult.get();
            userStatus.updateInfo(command);
            return UserStatusDto.from(userStatusRepository.save(userStatus));
        }

        return create(userId, new UserStatusCreateCommand(command.updateAt()));
    }

    public void delete(UUID userStatusId, UUID userId) {
        if(!userStatusRepository.existsByIdAndUser_Id(userStatusId, userId)) throw new UserStatusNotFoundException();
        userStatusRepository.deleteById(userStatusId);
    }

    private UserStatus getUserStatusRequireThrow(UUID userStatusId, UUID userId) {
        return userStatusRepository.findByIdAndUser_Id(userStatusId, userId)
                .orElseThrow(UserStatusNotFoundException::new);
    }

    private User getUserRequireThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private @NonNull UserStatusDto createUserStatus(UserStatusCreateCommand command, User user) {
        return UserStatusDto.from(userStatusRepository.save(new UserStatus(user, command)));
    }

}
