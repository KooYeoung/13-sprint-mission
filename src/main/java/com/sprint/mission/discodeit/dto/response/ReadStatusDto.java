package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReadStatusDto(
      UUID id
      , UUID userId
      , UUID channelId
      , OffsetDateTime readAt
) {

   public static ReadStatusDto from(ReadStatus readStatus) {
      return new ReadStatusDto(
            readStatus.getId()
            , readStatus.getUserId()
            , readStatus.getChannelId()
            , RequestTimeZoneUtils.toOffsetDateTime(readStatus.getUpdatedAt())
      );
   }

}
