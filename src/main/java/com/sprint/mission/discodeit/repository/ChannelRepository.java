package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
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
                select new com.sprint.mission.discodeit.dto.repository.ChannelSummary(
                            c.id, c.type, c.name, c.description, max(m.createdAt)
                            )
                from Channel c
                left join Message m on m.channel = c
                where c.type = :publicType or exists (
                    select 1
                    from ReadStatus r
                    where r.user.id = :userId
                        and r.channel.id = c.id
                    )
                group by c.id, c.type, c.name, c.description
            """)
    List<ChannelSummary> findVisibleChannels(@Param("userId") UUID userId,
                                             @Param("publicType") ChannelType publicType);
    @Query("""
                select new com.sprint.mission.discodeit.dto.repository.ChannelSummary(
                            c.id, c.type, c.name, c.description, max(m.createdAt)
                            )
                from Channel c
                left join Message m on m.channel = c
                where c.id = :channelId
                group by c.id, c.type, c.name, c.description
            """)
    Optional<ChannelSummary> findByDetail(@Param("channelId") UUID channelId);
}
