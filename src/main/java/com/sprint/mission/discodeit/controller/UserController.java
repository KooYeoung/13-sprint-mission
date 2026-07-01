package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.swagger.UserApi;
import com.sprint.mission.discodeit.dto.request.user.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.user.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.request.userStatus.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.basic.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController implements UserApi {

    private final UserService userService;
    private final UserStatusService userStatusService;

    @PostMapping
    public ResponseEntity<UserDto> create(
            @RequestPart UserCreateRequest userCreateRequest,
            @RequestPart(required = false) MultipartFile profile
    ) {
        UserDto userDto = userService.create(userCreateRequest.toCommand(), profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> list() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserDto> update(
            @PathVariable UUID userId,
            @RequestPart UserUpdateRequest userUpdateRequest,
            @RequestPart(required = false) MultipartFile profile
    ) {
        return ResponseEntity.ok(userService.update(userId, userUpdateRequest.toCommand(), profile));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable UUID userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/userStatus")
    public ResponseEntity<UserStatusDto> statusUpdate(@PathVariable UUID userId,
                                                      @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(userStatusService.updateByUserId(userId, request.toCommand()));
    }
}