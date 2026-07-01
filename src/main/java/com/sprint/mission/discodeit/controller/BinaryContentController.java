package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.swagger.BinaryContentApi;
import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.service.basic.BinaryContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
