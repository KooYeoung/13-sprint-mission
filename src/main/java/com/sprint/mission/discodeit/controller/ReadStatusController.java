package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.service.basic.ReadStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class ReadStatusController {

    private final ReadStatusService readStatusService;

    @RequestMapping(value = "/channels/{channelId}/read-statuses",method = RequestMethod.POST)
    public ResponseEntity<ReadStatusDto> save(@PathVariable UUID channelId, @RequestBody ReadStatusCreateRequest request){
        request = request.withChannelId(channelId);
        ReadStatusDto save = readStatusService.save(ReadStatusDto.from(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(save);
    }

    @RequestMapping(value ="/channels/{channelId}/read-statuses/{readStatusId}" ,method = RequestMethod.PUT)
    public ResponseEntity<ReadStatusDto> update(@PathVariable UUID channelId, @PathVariable UUID readStatusId, @RequestBody ReadStatusUpdateRequest request){
        request = request.withChannelId(channelId)
                .withId(readStatusId);

        ReadStatusDto update = readStatusService.update(ReadStatusDto.from(request));

        return ResponseEntity.ok().body(update);
    }

    @RequestMapping(value = "/users/{userId}/read-statuses",method = RequestMethod.GET)
    public ResponseEntity<List<ReadStatusDto>> listByUserId(@PathVariable UUID userId){
        List<ReadStatusDto> allByUserId = readStatusService.findAllByUserId(userId);

        return ResponseEntity.ok().body(allByUserId);
    }
}
