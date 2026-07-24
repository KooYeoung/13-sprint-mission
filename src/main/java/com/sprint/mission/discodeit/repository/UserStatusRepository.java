package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserStatusRepository extends JpaRepository<UserStatus, UUID> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT us FROM UserStatus us WHERE us.id = :userStatusId AND us.user.id = :userId")
    Optional<UserStatus> findByIdAndUserId(@Param("userStatusId") UUID userStatusId,@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<UserStatus> findByUser_Id(UUID userId);

    boolean existsByUser_Id(UUID userId);

    boolean existsByIdAndUser_Id(UUID userStatusId, UUID userId);

}
