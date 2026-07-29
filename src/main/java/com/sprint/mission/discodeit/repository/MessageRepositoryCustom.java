package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface MessageRepositoryCustom {

    Slice<Message> findAllByChannelId(UUID channelId, Pageable pageable, UUID cursor);
}
