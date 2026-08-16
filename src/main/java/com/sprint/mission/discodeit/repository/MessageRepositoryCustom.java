package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.dto.repository.MessagePagingCondition;
import com.sprint.mission.discodeit.entity.Message;
import org.springframework.data.domain.Slice;

public interface MessageRepositoryCustom {

    Slice<Message> findAllByCondition(MessagePagingCondition condition);
}
