package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReadStatusTest {
    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void constructors_initializeNotificationsFromChannelType(ChannelType type) {
        Channel channel = channel(type);
        User user = identified(user());
        Instant readAt = Instant.parse("2026-10-02T00:00:00Z");
        assertThat(new ReadStatus(channel, user, readAt).getNotificationEnabled()).isEqualTo(type == ChannelType.PRIVATE);
        assertThat(new ReadStatus(channel, user, new ReadStatusCreateCommand(user.getId(), readAt))
                .getNotificationEnabled()).isEqualTo(type == ChannelType.PRIVATE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void update_changesReadTimeAndNotificationPreference(boolean enabled) {
        ReadStatus status = new ReadStatus(channel(ChannelType.PRIVATE), user(), Instant.EPOCH);
        Instant updated = Instant.parse("2026-10-02T00:00:00Z");
        status.updateInfo(new ReadStatusUpdateCommand(updated, enabled));
        assertThat(status.getLastReadAt()).isEqualTo(updated);
        assertThat(status.getNotificationEnabled()).isEqualTo(enabled);
    }
}
