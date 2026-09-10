package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.config.SecurityConfig;
import com.sprint.mission.discodeit.security.LoginFailureHandler;
import com.sprint.mission.discodeit.security.LoginSuccessHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(AuthController.class)
@DisplayName("AuthController 슬라이스 테스트")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LoginSuccessHandler loginSuccessHandler;

    @MockitoBean
    LoginFailureHandler loginFailureHandler;

    @Test
    @DisplayName("CSRF 토큰 발급 요청 시 203 응답과 쿠키를 반환한다")
    void getCsrfToken_returnsCookie() throws Exception {
        String csrfTokenName = "XSRF-TOKEN";

        mockMvc.perform(get("/api/auth/csrf-token"))
                .andExpect(status().isNonAuthoritativeInformation())
                .andExpect(cookie().exists(csrfTokenName))
                .andExpect(cookie().httpOnly(csrfTokenName, false))
                .andExpect(cookie().path(csrfTokenName, "/"));
    }
}
