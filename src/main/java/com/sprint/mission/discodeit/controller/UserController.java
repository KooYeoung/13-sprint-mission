package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.request.user.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.user.UserLoginRequest;
import com.sprint.mission.discodeit.dto.request.user.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.basic.AuthService;
import com.sprint.mission.discodeit.service.basic.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/user")
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final UserStatusService userStatusService;
    private final AuthService authService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<UserDto> create(@RequestPart UserCreateRequest request
            , @RequestPart(required = false) MultipartFile file){

        UserDto userDto = userService.create(request.toCommand(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
    }

    @RequestMapping(value = "/findAll", method = RequestMethod.GET)
    public ResponseEntity<List<UserDto>> list(){
        List<UserDto> all = userService.findAll();
        return ResponseEntity.ok().body(all);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<UserDto> detailFind(@PathVariable UUID id){
        UserDto byId = userService.findById(id);

        return ResponseEntity.ok().body(byId);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<UserDto> update(@PathVariable UUID id, @RequestPart UserUpdateRequest request, @RequestPart(required = false) MultipartFile file){
        UserDto update = userService.update(id, request.toCommand(), file);

        return ResponseEntity.ok().body(update);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        userService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.PATCH)
    public ResponseEntity<UserStatusDto> statusUpdate(@PathVariable UUID userId){

        UserStatusDto update = userStatusService.updateByUserId(userId);

        return ResponseEntity.ok().body(update);
    }

    @RequestMapping(value = "/login",method = RequestMethod.POST)
    public ResponseEntity<UserDto> login(@RequestBody UserLoginRequest request){
        UserDto login = authService.login(request.toCommand());
        return ResponseEntity.ok().body(login);
    }
}
