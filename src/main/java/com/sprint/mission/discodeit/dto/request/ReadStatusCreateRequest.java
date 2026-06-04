package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.ReadStatus;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusCreateRequest(
      UUID userId
      , UUID channelId
      , Instant readAt
) {
   public ReadStatus toReadStatus() {

      return new ReadStatus(
            readAt
            , userId
            , channelId);
   }


}
