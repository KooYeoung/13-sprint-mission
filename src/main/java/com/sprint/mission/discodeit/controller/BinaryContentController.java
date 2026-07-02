package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.swagger.BinaryContentApi;
import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.DownloadDto;
import com.sprint.mission.discodeit.service.basic.BinaryContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/binaryContents")
@RestController
@RequiredArgsConstructor
@Slf4j
public class BinaryContentController implements BinaryContentApi {

    private final BinaryContentService binaryContentService;

    @GetMapping
    public ResponseEntity<List<BinaryContentDto>> listByIds(
            @RequestParam List<UUID> binaryContentIds
    ) {
        return ResponseEntity.ok(binaryContentService.findAllByIdIn(binaryContentIds));
    }

    @GetMapping("/{binaryContentId}")
    public ResponseEntity<BinaryContentDto> findById(
            @PathVariable UUID binaryContentId
    ) {
        return ResponseEntity.ok(binaryContentService.findById(binaryContentId));
    }

    @GetMapping("/{binaryContentId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID binaryContentId) {
        DownloadDto download = binaryContentService.download(binaryContentId);
        BinaryContentDto binaryContentDto = download.binaryContent();

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(binaryContentDto.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(getMediaType(binaryContentDto.contentType()))
                .contentLength(binaryContentDto.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(download.resource());
    }

    private @NonNull MediaType getMediaType(String  contentType) {
        return contentType== null ?
                MediaType.APPLICATION_OCTET_STREAM :
                MediaType.parseMediaType(contentType);
    }

}
