package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.swagger.MessageApi;
import com.sprint.mission.discodeit.dto.request.message.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.message.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController implements MessageApi {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageDto> create(
            @Valid @RequestPart MessageCreateRequest messageCreateRequest,
            @RequestPart(required = false) List<MultipartFile> attachments
    ) {
        MessageDto save = messageService.save(messageCreateRequest.toCommand(), attachments);
        return ResponseEntity.status(HttpStatus.CREATED).body(save);
    }

    @GetMapping
    public ResponseEntity<PageResponse<MessageDto>> listByChannelId(
            @RequestParam UUID channelId,
            @PageableDefault(
                    size = 50,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,
            @RequestParam(required = false) UUID cursor
    ) {

        log.info("cursor : {}", cursor);

        return ResponseEntity.ok(messageService.findAllByChannelId(channelId, pageable, cursor));
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageDto> update(
            @PathVariable UUID messageId,
            @Valid @RequestBody MessageUpdateRequest request
    ) {
        return ResponseEntity.ok(messageService.update(messageId, request.toCommand()));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID messageId) {
        messageService.delete(messageId);
        return ResponseEntity.noContent().build();
    }
}
