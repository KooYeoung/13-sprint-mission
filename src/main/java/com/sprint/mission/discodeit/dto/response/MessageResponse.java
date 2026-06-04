package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;

import java.util.List;
import java.util.UUID;

public record MessageResponse (
      UUID messageId
      , String content
      , List<UUID> fileIds
      , String nickname
      , UUID userId
      , UUID channelId
){
   public static MessageResponse from(Message message,String nickname) {
      return new MessageResponse(
            message.getId()
            , message.getContent()
            , message.getFileIds()
            , nickname
            , message.getUserId()
            , message.getChannelId()
            );
   }
}
