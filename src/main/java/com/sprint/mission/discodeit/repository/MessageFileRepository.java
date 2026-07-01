package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.MessageFile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageFileRepository extends JpaRepository<MessageFile, UUID> {
    boolean existsByMessage_Id(UUID messageId);

    void deleteByMessage_Id(UUID messageId);

    @EntityGraph(attributePaths = {"binaryContent"})
    List<MessageFile> findAllByMessage_Id(UUID messageId);
}
