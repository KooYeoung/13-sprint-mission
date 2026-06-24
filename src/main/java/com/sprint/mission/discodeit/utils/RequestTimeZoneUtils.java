package com.sprint.mission.discodeit.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestTimeZoneUtils {
    private static final String TIME_ZONE_HEADER = "Time-Zone";
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");

    public static OffsetDateTime toOffsetDateTime(Instant instant){
        if(instant == null){
            return  null;
        }

        return instant
                .atZone(currentZoneId())
                .toOffsetDateTime();
    }

    private static ZoneId currentZoneId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if(attributes == null){
            System.out.println("attributes = " + attributes);
            return DEFAULT_ZONE_ID;
        }

        HttpServletRequest request = attributes.getRequest();
        String timeZoneHeader = request.getHeader(TIME_ZONE_HEADER);

        return pareZoneId(timeZoneHeader);

    }

    private static ZoneId pareZoneId(String timeZoneHeader) {
        if(timeZoneHeader == null || timeZoneHeader.isBlank()){
            System.out.println("timeZoneHeader = " + timeZoneHeader);
            return DEFAULT_ZONE_ID;
        }

        try {
            System.out.println("timeZoneHeader = " + timeZoneHeader);
            return ZoneId.of(timeZoneHeader);
        }catch (DateTimeException e){
            return DEFAULT_ZONE_ID;
        }

    }


}
