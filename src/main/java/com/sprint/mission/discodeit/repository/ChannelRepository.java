package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    @Query("""
                select c.id
                from Channel c
                where c.type in :types or exists (
                    select 1
                    from ReadStatus r
                    where r.user.id = :userId
                        and r.channel.id = c.id
                    )
            """)
    List<UUID> findVisibleChannelIds(@Param("userId") UUID userId,
                                     @Param("types") List<ChannelType> types);

    @Query("""
                select new com.sprint.mission.discodeit.dto.repository.ChannelSummary(
                            c.id, c.type, c.name, c.description, grouped.lastMessageCreated
                            )
                from Channel c
                left join (
                                select m.channel.id as channelId , max(m.createdAt) lastMessageCreated
                                from Message m
                                where m.channel.id in :channelId
                                group by m.channel.id
                            ) as grouped on grouped.channelId = c.id
                where c.id = :channelId
            """)
    Optional<ChannelSummary> findByDetail(@Param("channelId") UUID channelId);

    @Query("""
                select new com.sprint.mission.discodeit.dto.repository.ChannelSummary(
                            c.id, c.type, c.name, c.description, grouped.lastMessageCreated
                            )
                from Channel c
                left join (
                                select m.channel.id as channelId , max(m.createdAt) lastMessageCreated
                                from Message m
                                where m.channel.id in :channelIds
                                group by m.channel.id
                            ) as grouped on grouped.channelId = c.id
               where c.id in :channelIds
            """)
    List<ChannelSummary> findSummariesByIdIn(@Param("channelIds") List<UUID> channelIds);

    @Query("""
        select (count(c) > 0)
        from Channel c
        where c.id = :channelId
            and (c.type = com.sprint.mission.discodeit.entity.ChannelType.PUBLIC or exists (
                select 1
                from ReadStatus r
                where r.channel.id = c.id
                    and r.user.id = :userId
                ))
    """)
    boolean isChannelAccessible(@Param("channelId") UUID channelId, @Param("userId") UUID userId);
}
