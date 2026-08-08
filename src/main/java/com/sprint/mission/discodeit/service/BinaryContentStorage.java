package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface BinaryContentStorage {
    UUID put(UUID fileId, InputStream bytes);

    InputStream get(UUID fileId);

    void delete(UUID fileId);

    Resource download(BinaryContentDto binaryContentDto);

    void deleteAll(List<UUID> binaryContentIds);

}
