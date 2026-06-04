package com.sprint.mission.discodeit.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public class BinaryContent extends BaseEntity{
   private final String fileName;
   private final String originalFileName;
   private final String contentType;

   @Builder
   public BinaryContent(
         String fileName
         , String originalFileName
         , String contentType) {
      super(Instant.now());
      this.fileName = fileName;
      this.originalFileName = originalFileName;
      this.contentType = contentType;
   }

}
