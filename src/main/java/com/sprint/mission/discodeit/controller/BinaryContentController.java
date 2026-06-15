package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.service.basic.BinaryContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/binaryContent")
@RestController
@RequiredArgsConstructor
@Slf4j
public class BinaryContentController {

    private final BinaryContentService binaryContentService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<BinaryContentDto>> listByIds(@RequestParam List<UUID> ids){
        List<BinaryContentDto> allByIdIn = binaryContentService.findAllByIdIn(ids);

        return ResponseEntity.ok().body(allByIdIn);
    }

    @RequestMapping(value = "/find",method = RequestMethod.GET)
    public ResponseEntity<BinaryContentDto> findById(@RequestParam UUID binaryContentId){
        BinaryContentDto binaryContentDto = binaryContentService.findById(binaryContentId);

        return ResponseEntity.ok().body(binaryContentDto);
    }
}
