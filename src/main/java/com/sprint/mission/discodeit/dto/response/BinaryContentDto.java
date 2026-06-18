package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.BinaryContent;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record BinaryContentDto(
      UUID id
      , String fileName
      , String contentType
      , String originalFileName
      ,Long size
      ,String savePath
) {

   public static BinaryContentDto from(BinaryContent binaryContent)  {

       return new BinaryContentDto(
            binaryContent.getId()
            , binaryContent.getFileName()
            , binaryContent.getContentType()
            , binaryContent.getOriginalFileName()
              , binaryContent.getSize()
              ,binaryContent.getPath()

            );
   }

}
