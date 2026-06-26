package com.sprint.mission.discodeit.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomBadRequestException.class)
    public ResponseEntity<ApiErrorResponse> badRequest(RuntimeException e) {
        log.warn(e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(CustomNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(RuntimeException e) {
        log.warn(e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldsMap = new HashMap<>();

        e.getBindingResult()
                .getFieldErrors()
                .forEach(field -> fieldsMap.put(field.getField(), field.getDefaultMessage()));

        log.warn("validation errors={}", fieldsMap);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("요청 본문에 일부 필드가 유효하지 않습니다.", fieldsMap));
    }

    @ExceptionHandler(CustomInternalServerException.class)
    public ResponseEntity<ApiErrorResponse> internalServerError(CustomInternalServerException e) {
        log.error("internal server error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> exception(Exception e) {
        log.error("Unhandled exception", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("알 수 없는 오류가 발생했습니다."));
    }

}
