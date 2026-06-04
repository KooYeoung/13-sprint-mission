package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusResponse(
      UUID id
      , UUID userId
      , UUID channelId
      , Instant readAt
) {

   public static ReadStatusResponse from(ReadStatus readStatus) {
      return new ReadStatusResponse(
            readStatus.getId()
            , readStatus.getUserId()
            , readStatus.getChannelId()
            , readStatus.getUpdatedAt()
      );
   }
}
