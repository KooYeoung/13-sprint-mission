package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.util.UUID;

public interface BinaryContentStorage {
    UUID put(UUID fileId, byte[] bytes);

    InputStream get(UUID fileId);

    void delete(UUID fileId);

    Resource download(BinaryContentDto binaryContentDto);
}
