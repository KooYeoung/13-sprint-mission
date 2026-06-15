package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.ReadStatus;

import java.time.Instant;
import java.util.UUID;

public record ReadStatusDto(
      UUID id
      , UUID userId
      , UUID channelId
      , Instant readAt
) {

   public static ReadStatusDto from(ReadStatus readStatus) {
      return new ReadStatusDto(
            readStatus.getId()
            , readStatus.getUserId()
            , readStatus.getChannelId()
            , readStatus.getUpdatedAt()
      );
   }

   public static ReadStatusDto from(ReadStatusCreateRequest request){
      return new ReadStatusDto(
              null,
              request.userId(),
              request.channelId(),
              request.readAt()
      );
   }

   public static ReadStatusDto from(ReadStatusUpdateRequest request){
      return new ReadStatusDto(
              request.id(),
              request.userId(),
              request.channelId(),
              request.readAt()
      );
   }
}
