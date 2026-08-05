package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

public record RedirectDownloadResult(URI location) implements DownloadResult {
    @Override
    public ResponseEntity<Resource> toResponseEntity(BinaryContentDto metadata) {
        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .location(location)
                .build();
    }
}
