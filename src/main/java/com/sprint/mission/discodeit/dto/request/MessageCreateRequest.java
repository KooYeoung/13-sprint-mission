package com.sprint.mission.discodeit.dto.request;

import com.sprint.mission.discodeit.entity.Message;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MessageCreateRequest(
      String content
      , UUID channelId
      , UUID userId
) {

}
