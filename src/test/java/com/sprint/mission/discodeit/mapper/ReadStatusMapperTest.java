package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.entity.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.ZoneOffset;

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReadStatusMapperTest {
    private final ReadStatusMapper mapper = Mappers.getMapper(ReadStatusMapper.class);

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void mapsIdsTimesAndNotificationPreference(boolean enabled) {
        User user = identified(user());
        Channel channel = identified(channel(ChannelType.PUBLIC));
        Instant time = Instant.parse("2026-10-02T00:00:00Z");
        ReadStatus status = identified(new ReadStatus(channel, user, time));
        status.updateInfo(new ReadStatusUpdateCommand(time, enabled));
        ReflectionTestUtils.setField(status, "createdAt", time);
        ReflectionTestUtils.setField(status, "updatedAt", time);

        ReadStatusDto dto = mapper.toDto(status);
        assertThat(dto).isEqualTo(new ReadStatusDto(status.getId(), time.atOffset(ZoneOffset.ofHours(9)),
                time.atOffset(ZoneOffset.ofHours(9)), user.getId(), channel.getId(), time.atOffset(ZoneOffset.ofHours(9)), enabled));
    }
}
