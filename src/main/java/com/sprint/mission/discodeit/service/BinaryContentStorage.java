package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import com.sprint.mission.discodeit.storage.DownloadResult;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface BinaryContentStorage {
    UUID put(UUID fileId, BinaryContentUpload upload);

    InputStream get(UUID fileId);

    void delete(UUID fileId);

    DownloadResult download(BinaryContentDto binaryContentDto);

    void deleteAll(List<UUID> binaryContentIds);

}
