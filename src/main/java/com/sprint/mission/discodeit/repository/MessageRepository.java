package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Override
    @EntityGraph(attributePaths = {"author", "channel", "author.userStatus", "author.profile"})
    Optional<Message> findById(UUID id);

    @EntityGraph(attributePaths = {"author", "channel", "author.userStatus", "author.profile"})
    Slice<Message> findAllByChannel_Id(UUID channelId, Pageable pageable);

    Optional<Message> findTop1ByChannel_IdOrderByCreatedAtDesc(UUID channelId);

    void deleteAllByChannel_Id(UUID channelId);

    boolean existsByChannel_Id(UUID channelId);
}
