package com.sprint.mission.discodeit.storage;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public record ResourceDownloadResult(Resource resource) implements DownloadResult {
    @Override
    public ResponseEntity<Resource> toResponseEntity(BinaryContentDto metadata) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(metadata.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(getMediaType(metadata.contentType()))
                .contentLength(metadata.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }


    private MediaType getMediaType(String contentType) {
        return contentType == null ?
                MediaType.APPLICATION_OCTET_STREAM :
                MediaType.parseMediaType(contentType);
    }
}
