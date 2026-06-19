package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReadStatusDto(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID userId,
        UUID channelId,
        OffsetDateTime lastReadAt
) {
   public static ReadStatusDto from(ReadStatus readStatus) {
      return new ReadStatusDto(
              readStatus.getId(),
              RequestTimeZoneUtils.toOffsetDateTime(readStatus.getCreatedAt()),
              RequestTimeZoneUtils.toOffsetDateTime(readStatus.getUpdatedAt()),
              readStatus.getUserId(),
              readStatus.getChannelId(),
              RequestTimeZoneUtils.toOffsetDateTime(readStatus.getUpdatedAt())
      );
   }
}