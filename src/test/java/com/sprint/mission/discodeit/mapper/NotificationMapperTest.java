package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.dto.response.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {
    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void mapsAllFieldsAndRequestTimeZone() {
        User receiver = identified(user());
        Notification notification = identified(Notification.of(new NotificationCreateCommand(List.of(receiver.getId()), "title", "content"), receiver));
        Instant createdAt = Instant.parse("2026-10-02T00:00:00Z");
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Time-Zone", "UTC");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            assertThat(mapper.toDto(notification)).isEqualTo(new NotificationDto(notification.getId(),
                    createdAt.atOffset(ZoneOffset.UTC), receiver.getId(), "title", "content"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
