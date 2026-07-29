package com.sprint.mission.discodeit.exception;

import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ApiErrorResponse(
        int status,
        String exceptionType,
        String code,
        String message,
        Map<String, Object> details,
        OffsetDateTime timestamp
) {
    public static ApiErrorResponse of(DiscodeitException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        return of(
                errorCode.getStatus().value(),
                exception.getClass().getSimpleName(),
                errorCode.getCode(),
                errorCode.getMessage(),
                exception.getDetails()
        );
    }

    public static ApiErrorResponse of(
            int status,
            String exceptionType,
            String code,
            String message,
            Map<String, Object> details
    ) {
        return new ApiErrorResponse(
                status,
                exceptionType,
                code,
                message,
                copyDetails(details),
                RequestTimeZoneUtils.toOffsetDateTime(Instant.now())
        );
    }

    private static Map<String, Object> copyDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> normalized = new LinkedHashMap<>();

        details.forEach((key, value) -> {
            if (key != null) {
                normalized.put(
                        key,
                        Objects.requireNonNullElse(value, "상세 정보가 없습니다.")
                );
            }
        });

        return Map.copyOf(normalized);
    }
}
