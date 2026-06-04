package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.BinaryContent;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

public record BinaryContentCreateRequest(
      MultipartFile file
) {
   public BinaryContent toBinaryContent() {
      return new BinaryContent(
            UUID.randomUUID().toString().replace("-", "")
            , file.getOriginalFilename()
            , file.getContentType());
   }
}
