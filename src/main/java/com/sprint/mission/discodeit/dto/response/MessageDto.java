package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Message;

import java.util.List;
import java.util.UUID;

public record MessageDto(
      UUID messageId
      , String content
      , List<UUID> fileIds
      , String nickname
      , UUID userId
      , UUID channelId
){
   public static MessageDto from(Message message, String nickname) {
      return new MessageDto(
            message.getId()
            , message.getContent()
            , message.getFileIds()
            , nickname
            , message.getUserId()
            , message.getChannelId()
            );
   }
}
