package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Override
    @EntityGraph(attributePaths = {"author", "channel", "author.userStatus"})
    Optional<Message> findById(UUID id);

    @EntityGraph(attributePaths = {"author", "channel", "author.userStatus"})
    Slice<Message> findAllByChannel_Id(UUID channelId, Pageable pageable);

    List<Message> findAllByChannel_Id(UUID channelId);

    Optional<Message> findTop1ByChannel_IdOrderByCreatedAtDesc(UUID channelId);

    @Query("""
                select m
                from Message m 
                where m.channel.id in :channelIds
                    and m.createdAt = (
                            select max( m2.createdAt) 
                            from Message m2
                            where m.channel.id = m2.channel.id
                        )
            """)
    List<Message> findLatestMessagesByChannelIds(@Param("channelIds") List<UUID> channelIds);
}
