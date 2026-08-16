package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.swagger.AuthApi;
import com.sprint.mission.discodeit.dto.request.user.UserLoginRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.service.basic.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<UserDto> login( @Valid @RequestBody UserLoginRequest request) {
        return ResponseEntity.ok(authService.login(request.toCommand()));
    }
}