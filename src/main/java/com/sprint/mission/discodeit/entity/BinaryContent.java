package com.sprint.mission.discodeit.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString
public class BinaryContent extends BaseEntity{
   private final String fileName;
   private final String originalFileName;
   private final String contentType;

   @Builder
   public BinaryContent(
          String originalFileName
         , String contentType) {
      super(Instant.now());
      this.originalFileName = originalFileName;
      this.contentType = contentType;
      this.fileName = createFileName();
   }

   private String createFileName(){
      String formattedId = this.getId().toString().replace("-", "");
      int lastDotIndex = this.originalFileName.lastIndexOf(".");
      if(lastDotIndex < 0){
         return formattedId;
      }
      String expansion = this.originalFileName.substring(lastDotIndex);
      return formattedId + expansion;
   }

}
