package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import org.springframework.stereotype.Component;

@Component
public class ReadStatusMapper {

    public ReadStatusDto toDto(ReadStatus readStatus) {
        return new ReadStatusDto(
                readStatus.getId(),
                RequestTimeZoneUtils.toOffsetDateTime(readStatus.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(readStatus.getUpdatedAt()),
                readStatus.getUserId(),
                readStatus.getChannelId(),
                RequestTimeZoneUtils.toOffsetDateTime(readStatus.getLastReadAt())
        );
    }
}
