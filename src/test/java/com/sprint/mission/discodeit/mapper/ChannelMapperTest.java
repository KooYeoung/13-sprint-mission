package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Role;
import com.sprint.mission.discodeit.security.JwtRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ChannelMapperImpl.class, ChannelParticipantMapper.class,
        UserMapperImpl.class, BinaryContentMapperImpl.class, UserOnlineMapper.class})
class ChannelMapperTest {
    @Autowired ChannelMapper mapper;
    @MockitoBean JwtRegistry registry;

    @AfterEach
    void clearRequest() { RequestContextHolder.resetRequestAttributes(); }

    @Test
    void combinesSummaryAndParticipantsUsingTheCurrentRequestTimeZone() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Time-Zone", "UTC");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        Instant instant = Instant.parse("2026-10-06T00:00:00Z");
        ChannelSummary summary = new ChannelSummary(UUID.randomUUID(), ChannelType.PUBLIC, "general", "description", instant);
        OffsetDateTime cachedTime = instant.atOffset(ZoneOffset.ofHours(9));
        UserDto cached = new UserDto(UUID.randomUUID(), "member", "member@test.local", null, true,
                Role.USER, cachedTime, cachedTime);

        var result = mapper.toDtoWithParticipants(summary, List.of(cached));

        assertThat(result.id()).isEqualTo(summary.id());
        assertThat(result.type()).isEqualTo(summary.type());
        assertThat(result.name()).isEqualTo(summary.name());
        assertThat(result.description()).isEqualTo(summary.description());
        assertThat(result.lastMessageAt()).isEqualTo(instant.atOffset(ZoneOffset.UTC));
        assertThat(result.participants()).singleElement().satisfies(participant -> {
            assertThat(participant.id()).isEqualTo(cached.id());
            assertThat(participant.online()).isTrue();
            assertThat(participant.createdAt().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(participant.updatedAt().toInstant()).isEqualTo(instant);
        });
        assertThat(cached.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(mapper.toDtoWithParticipants(new ChannelSummary(summary.id(), ChannelType.PRIVATE,
                null, null, null), List.of()).lastMessageAt()).isNull();
    }
}
