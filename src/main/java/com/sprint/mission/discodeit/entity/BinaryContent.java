package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString
public class BinaryContent extends BaseEntity{
   private final String fileName;
   private final String originalFileName;
   private final String contentType;
   private final Long size;
   private final String path;

   public BinaryContent(String originalFileName, String fileName, String contentType, Long size, String path) {
      super(Instant.now());
      this.fileName = fileName;
      this.originalFileName = originalFileName;
      this.contentType = contentType;
      this.size = size;
      this.path = path;
   }


}
