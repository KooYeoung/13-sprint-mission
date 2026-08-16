package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.swagger.ChannelApi;
import com.sprint.mission.discodeit.dto.request.channel.ChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.request.channel.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.channel.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.service.ChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@Slf4j
@RestController
public class ChannelController implements ChannelApi {

    private final ChannelService channelService;

    @PostMapping("/public")
    public ResponseEntity<ChannelDto> createPublic( @Valid @RequestBody PublicChannelCreateRequest request) {
        ChannelDto save = channelService.save(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(save);
    }

    @PostMapping("/private")
    public ResponseEntity<ChannelDto> createPrivate( @Valid @RequestBody PrivateChannelCreateRequest request) {
        ChannelDto save = channelService.save(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(save);
    }

    @GetMapping
    public ResponseEntity<List<ChannelDto>> listByUserId(@RequestParam UUID userId) {
        return ResponseEntity.ok(channelService.findAllByUserId(userId));
    }

    @PatchMapping("/{channelId}")
    public ResponseEntity<ChannelDto> update(
            @PathVariable UUID channelId,
            @Valid @RequestBody ChannelUpdateRequest request
    ) {
        return ResponseEntity.ok(channelService.update(channelId, request.toCommand()));
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> delete(@PathVariable UUID channelId) {
        channelService.delete(channelId);
        return ResponseEntity.noContent().build();
    }
}