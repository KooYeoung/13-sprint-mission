package com.sprint.mission.discodeit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.command.user.UserLoginCommand;
import com.sprint.mission.discodeit.dto.request.ValidationMessage;
import com.sprint.mission.discodeit.dto.request.user.UserLoginRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.service.basic.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController 슬라이스 테스트")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @Test
    @DisplayName("로그인 성공 - 유효한 인증 요청이면 200 OK와 사용자 정보 반환")
    void login_returnsOkAndUser_whenCredentialsAreValid() throws Exception {
        // given
        // Controller 슬라이스 테스트의 대상은 HTTP 요청 바인딩, Bean Validation 통과 여부,
        // 응답 status/body 직렬화, Service 호출 계약이다.
        // AuthService 자체의 인증 로직은 여기서 검증하지 않고 @MockitoBean으로 대체한다.
        UserLoginRequest request = new UserLoginRequest(
                "testUser",
                "testPassword"
        );

        // 서비스가 반환할 UserDto를 실제 record로 만든다.
        // profile은 null로 둬도 로그인 응답 직렬화 계약을 검증할 수 있고,
        // profile 상세 매핑은 UserDto/Mapper 쪽 테스트 책임으로 남긴다.
        UUID userId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-28T10:15:30+09:00");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-07-28T10:20:30+09:00");
        UserDto response = new UserDto(
                userId,
                request.username(),
                "test@gmail.com",
                null,
                true,
                createdAt,
                updatedAt
        );

        // request.toCommand()가 새 record 인스턴스를 만들기 때문에 동일 객체 여부가 아니라
        // command 필드 값이 정확히 전달되는지를 ArgumentCaptor로 확인한다.
        given(authService.login(any(UserLoginCommand.class))).willReturn(response);

        String requestBody = objectMapper.writeValueAsString(request);

        //
        // when
        // POST /api/auth/login 요청을 application/json으로 전송한다.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))

        // then
        // 응답 상태가 200 OK인지 확인한다.
        // 응답 body가 authService가 반환한 UserDto 필드를 JSON으로 포함하는지 확인한다.
        // 날짜는 Jackson의 JavaTimeModule 설정을 거친 문자열로 직렬화되므로 DTO 값과 같은 ISO-8601 표현인지 검증한다.
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value(response.username()))
                .andExpect(jsonPath("$.email").value(response.email()))
                .andExpect(jsonPath("$.profile").doesNotExist())
                .andExpect(jsonPath("$.online").value(response.online()))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()));

        // Controller가 request body를 UserLoginRequest로 바인딩한 뒤,
        // AuthService에 username/password가 보존된 command를 넘겼는지 확인한다.
        ArgumentCaptor<UserLoginCommand> commandCaptor = ArgumentCaptor.forClass(UserLoginCommand.class);
        verify(authService).login(commandCaptor.capture());

        UserLoginCommand command = commandCaptor.getValue();
        assertThat(command.username()).isEqualTo(request.username());
        assertThat(command.password()).isEqualTo(request.password());
    }

    @Test
    @DisplayName("로그인 실패 - 필수값이 비어 있으면 400 Bad Request 반환")
    void login_returnsBadRequest_whenRequestIsInvalid() throws Exception {
        // given
        // username과 password는 @NotBlank 대상이다.
        // 두 값을 모두 blank로 보내면 Controller 진입 전 Bean Validation이 실패해야 한다.
        // 이때 인증 시도 자체가 일어나면 안 되므로 Service는 stub을 만들지 않는다.
        UserLoginRequest request = new UserLoginRequest(
                "",
                " "
        );

        String requestBody = objectMapper.writeValueAsString(request);
        //
        // when
        // POST /api/auth/login 요청을 application/json으로 전송한다.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody))

        // then
        // Bean Validation 실패로 400 Bad Request가 반환되는지 확인한다.
        // GlobalExceptionHandler가 내려주는 validation error body 구조도 함께 확인한다.
        // details에는 field name을 key로, ValidationMessage 값을 배열 value로 담는다.
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.exceptionType").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 데이터가 올바르지 않습니다."))
                .andExpect(jsonPath("$.details.username").isArray())
                .andExpect(jsonPath("$.details.username[0]").value(ValidationMessage.USER_NAME))
                .andExpect(jsonPath("$.details.password").isArray())
                .andExpect(jsonPath("$.details.password[0]").value(ValidationMessage.USER_PASSWORD))
                .andExpect(jsonPath("$.timestamp").exists());

        // validation이 실패한 요청은 AuthController.login(...) 본문까지 도달하지 않아야 한다.
        // 따라서 AuthService.login(...) 호출이 한 번도 없어야 한다.
        verify(authService, never()).login(any(UserLoginCommand.class));
    }
}
