package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.UserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStatusRepository {
   UserStatus save(UserStatus userStatus);
   Optional<UserStatus> findByIdAndUserId(UUID userStatusId, UUID userId);
   Optional<UserStatus> findByUserId(UUID userId);
   List<UserStatus> findAll();
   UserStatus update(UserStatus userStatus);
   void delete(UUID id);
}
