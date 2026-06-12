package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
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

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<MessageDto> create(@RequestPart MessageCreateRequest request, @RequestPart(required = false) List<MultipartFile> files){
        MessageDto save = messageService.save(MessageDto.from(request), files);

        return ResponseEntity.status(HttpStatus.CREATED).body(save);
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.PUT)
    public ResponseEntity<MessageDto> update(@RequestBody MessageUpdateRequest request, @PathVariable UUID id){
        request = request.withMessageId(id);
        MessageDto update = messageService.update(MessageDto.from(request));
        return ResponseEntity.ok().body(update);
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        messageService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/channel/{channelId}",method = RequestMethod.GET)
    public ResponseEntity<List<MessageDto>> listByChannelId(@PathVariable UUID channelId){
        List<MessageDto> allByChannelId = messageService.findAllByChannelId(channelId);

        return ResponseEntity.ok().body(allByChannelId);
    }
}
