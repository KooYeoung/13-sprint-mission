package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.channel.ChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.channel.ChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.service.ChannelService;
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
public class ChannelController {

    private final ChannelService channelService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<ChannelDto> create(@RequestBody ChannelCreateRequest request){
        ChannelDto save = channelService.save(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(save);
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.PUT)
    public ResponseEntity<ChannelDto> update(@PathVariable UUID id, @RequestBody ChannelUpdateRequest request){
        ChannelDto update = channelService.update(id, request.toCommand());
        return ResponseEntity.ok().body(update);
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        channelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/user/{userId}",method = RequestMethod.GET)
    public ResponseEntity<List<ChannelDto>> listByUserId(@PathVariable UUID userId){
        List<ChannelDto> allByUserId = channelService.findAllByUserId(userId);

        return ResponseEntity.ok().body(allByUserId);
    }
}

