package com.sprint.mission.discodeit.exception;

import com.sprint.mission.discodeit.exception.storage.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DiscodeitException.class)
    public ResponseEntity<ApiErrorResponse> handleDiscodeitException(DiscodeitException e) {
        HttpStatus status = e.getErrorCode().getStatus();

        if (status.is5xxServerError()) {
            logServerError(e);
        } else {
            log.warn(
                    "client error. code={}, message={}",
                    e.getErrorCode(),
                    e.getMessage()
            );
        }

        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> fieldsMap = new HashMap<>();

        e.getBindingResult()
                .getFieldErrors()
                .forEach(field -> fieldsMap.put(field.getField(), field.getDefaultMessage()));

        log.warn("validation errors={}", fieldsMap);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiErrorResponse.of(
                        errorCode.getStatus().value(),
                        e.getClass().getSimpleName(),
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        fieldsMap
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> exception(Exception e) {
        log.error("Unhandled exception", e);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiErrorResponse.of(
                        errorCode.getStatus().value(),
                        e.getClass().getSimpleName(),
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        Map.of()
                ));
    }

    private void logServerError(DiscodeitException e) {
        if (e instanceof StorageException storageException) {
            log.error(
                    "storage error. code={}, path={}",
                    e.getErrorCode(),
                    getLogPath(storageException),
                    e
            );
            return;
        }

        log.error(
                "application error. code={}, message={}",
                e.getErrorCode(),
                e.getMessage(),
                e
        );
    }

    private Path getLogPath(StorageException storageException) {
        if (storageException.getPath() == null) {
            return null;
        }
        return storageException.getPath().getFileName();
    }

}
