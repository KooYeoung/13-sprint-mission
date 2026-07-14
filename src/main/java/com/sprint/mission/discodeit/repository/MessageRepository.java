package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> , MessageRepositoryCustom{

    @Override
    @EntityGraph(attributePaths = {"author", "channel", "author.userStatus", "author.profile"})
    Optional<Message> findById(UUID id);

    @EntityGraph(attributePaths = {"author", "channel", "author.userStatus", "author.profile"})
    Slice<Message> findAllByChannel_Id(UUID channelId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "channel", "author.userStatus", "author.profile"})
    @Query("""
        select m
        from Message m
        where m.channel.id = :channelId
            and m.createdAt < :cursor
    """)
    Slice<Message> findAllByChannelIdWithCursor(@Param("channelId") UUID channelId, Pageable pageable,@Param("cursor") Instant cursor);

    Optional<Message> findTop1ByChannel_IdOrderByCreatedAtDesc(UUID channelId);

    void deleteAllByChannel_Id(UUID channelId);

    boolean existsByChannel_Id(UUID channelId);

    @Modifying(flushAutomatically = true,clearAutomatically = true)
    @Query("""
        update Message m
        set
            m.author = null 
        where
            m.author.id = :authorId
               
    """)
    void detachAuthorByAuthorId(@Param("authorId") UUID authorId);

    boolean existsByAuthor_Id(UUID authorId);
}
