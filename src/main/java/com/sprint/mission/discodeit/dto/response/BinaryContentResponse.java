package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.BinaryContent;
import java.util.UUID;

public record BinaryContentResponse(
      UUID id
      , String fileName
      , String contentType
      , String originalFileName
) {

   public static BinaryContentResponse from(BinaryContent binaryContent) {
      return new BinaryContentResponse(
            binaryContent.getId()
            , binaryContent.getFileName()
            , binaryContent.getContentType()
            , binaryContent.getOriginalFileName()
            );
   }
}
