package com.sprint.mission.discodeit.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

@Tag(name = "Auth", description = "인증 API")
public interface AuthApi {

    @Operation(summary = "csrf-token 발급")
    ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken);
}
