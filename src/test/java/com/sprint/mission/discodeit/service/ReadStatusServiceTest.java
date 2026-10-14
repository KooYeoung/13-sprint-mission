package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.ReadStatusMapper;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.basic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadStatusServiceTest {
    @Mock ReadStatusRepository repository;
    @Mock ReadStatusMapper mapper;
    @Mock ChannelReader channelReader;
    @Mock UserReader userReader;
    @InjectMocks ReadStatusService service;

    @ParameterizedTest
    @EnumSource(ChannelType.class)
    void saveAll_passesChannelPreferenceAndDistinctParticipants(ChannelType type) {
        Channel channel = identified(channel(type));
        UUID userId = UUID.randomUUID();
        given(repository.bulkInsert(channel.getId(), List.of(userId), Instant.EPOCH, channel.isPrivate())).willReturn(1);
        service.saveAll(channel, List.of(userId, userId), Instant.EPOCH);
        verify(repository).bulkInsert(channel.getId(), List.of(userId), Instant.EPOCH, type == ChannelType.PRIVATE);
    }

    @Test
    void saveAll_rejectsMissingUsersFromInsertedRowCount() {
        Channel channel = identified(channel(ChannelType.PRIVATE));
        List<UUID> userIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        given(repository.bulkInsert(channel.getId(), userIds, Instant.EPOCH, true)).willReturn(1);
        assertThatThrownBy(() -> service.saveAll(channel, userIds, Instant.EPOCH)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void saveAll_rejectsEmptyParticipantsWithoutDatabaseCall() {
        assertThatThrownBy(() -> service.saveAll(channel(ChannelType.PRIVATE), List.of(), Instant.EPOCH))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void update_changesActualReadStatus() {
        ReadStatus status = identified(new ReadStatus(channel(ChannelType.PRIVATE), user(), Instant.EPOCH));
        given(repository.findById(status.getId())).willReturn(Optional.of(status));
        given(repository.save(status)).willReturn(status);
        Instant updated = Instant.parse("2026-10-02T00:00:00Z");
        service.update(status.getId(), new ReadStatusUpdateCommand(updated, false));
        assertThat(status.getLastReadAt()).isEqualTo(updated);
        assertThat(status.getNotificationEnabled()).isFalse();
    }
}
