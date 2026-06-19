package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MessageDto(
      UUID messageId
      , String content
      , List<UUID> fileIds
      , String nickname
      , UUID userId
      , UUID channelId
      , OffsetDateTime createdAt
      , OffsetDateTime updatedAt
){
   public static MessageDto from(Message message, String nickname) {
      return new MessageDto(
            message.getId()
            , message.getContent()
            , message.getFileIds()
            , nickname
            , message.getUserId()
            , message.getChannelId()
              , RequestTimeZoneUtils.toOffsetDateTime(message.getCreatedAt())
              ,RequestTimeZoneUtils.toOffsetDateTime(message.getUpdatedAt())
            );
   }
}
