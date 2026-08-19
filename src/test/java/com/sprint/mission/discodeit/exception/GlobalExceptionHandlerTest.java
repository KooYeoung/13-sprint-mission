package com.sprint.mission.discodeit.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("validation details keep all messages for the same field")
    void handleValidation_groupsMessagesByField() throws Exception {
        // given
        Method method = ValidationTarget.class.getDeclaredMethod("validate", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new ValidationTarget(), "validationTarget");
        bindingResult.addError(new FieldError("validationTarget", "username", "first message"));
        bindingResult.addError(new FieldError("validationTarget", "username", "second message"));

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        // when
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleValidation(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().details())
                .containsEntry("username", List.of("first message", "second message"));
    }

    private static class ValidationTarget {
        @SuppressWarnings("unused")
        void validate(String username) {
        }
    }
}
