package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReadStatusRepository extends JpaRepository<ReadStatus, UUID> {

    List<ReadStatus> findByUser_Id(UUID userId);

    void deleteByChannel_Id(UUID channelId);

    boolean existsByChannel_IdAndUser_Id(UUID channelId, UUID userId);

}
