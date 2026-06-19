package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.message.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.message.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ResponseEntity<MessageDto> create(
            @RequestPart MessageCreateRequest messageCreateRequest,
            @RequestPart( required = false) List<MultipartFile> attachments
    ) {
        MessageDto save = messageService.save(messageCreateRequest.toCommand(), attachments);
        return ResponseEntity.status(HttpStatus.CREATED).body(save);
    }

    @GetMapping
    public ResponseEntity<List<MessageDto>> listByChannelId(@RequestParam UUID channelId) {
        return ResponseEntity.ok(messageService.findAllByChannelId(channelId));
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageDto> update(
            @PathVariable UUID messageId,
            @RequestBody MessageUpdateRequest request
    ) {
        return ResponseEntity.ok(messageService.update(messageId, request.toCommand()));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID messageId) {
        messageService.delete(messageId);
        return ResponseEntity.noContent().build();
    }
}