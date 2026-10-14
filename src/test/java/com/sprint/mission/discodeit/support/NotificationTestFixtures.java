package com.sprint.mission.discodeit.support;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePrivateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePublicCommand;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class NotificationTestFixtures {
    private NotificationTestFixtures() {}

    public static User user() {
        String suffix = UUID.randomUUID().toString();
        return new User(new UserCreateCommand("user-" + suffix, "encoded-password", suffix + "@test.local"), null);
    }

    public static Channel channel(ChannelType type) {
        return type == ChannelType.PRIVATE
                ? new Channel(new ChannelCreatePrivateCommand(List.of(), type))
                : new Channel(new ChannelCreatePublicCommand("test-channel", "description", type));
    }

    // 단위 테스트용 식별자이며 실제 DB 저장을 의미하지 않는다.
    public static <T> T identified(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    public static DiscodeitUserDetails principal(User user) {
        OffsetDateTime now = OffsetDateTime.parse("2026-10-02T00:00:00Z");
        return new DiscodeitUserDetails(new UserDto(user.getId(), user.getUsername(), user.getEmail(),
                null, false, user.getRole(), now, now), user.getPassword());
    }
}
