package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    @Query("""
                select c 
                from Channel c
                where c.type = :publicType or exists (
                    select 1
                    from ReadStatus r
                    where r.user.id = :userId
                        and r.channel.id = c.id
                    )
            """)
    List<Channel> findVisibleChannels(@Param("userId") UUID userId,
                                      @Param("publicType") ChannelType publicType);
}
