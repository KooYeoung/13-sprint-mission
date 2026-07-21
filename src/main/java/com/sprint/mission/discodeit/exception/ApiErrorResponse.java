package com.sprint.mission.discodeit.exception;

import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

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
                details == null ? Map.of() : Map.copyOf(details),
                RequestTimeZoneUtils.toOffsetDateTime(Instant.now())
        );
    }
}
