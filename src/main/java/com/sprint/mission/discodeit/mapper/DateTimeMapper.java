package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.OffsetDateTime;

public class DateTimeMapper {

    @Named("toOffsetDateTime")
    public static OffsetDateTime toOffsetDateTime(Instant instant) {
        return RequestTimeZoneUtils.toOffsetDateTime(instant);
    }
}
