package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.entity.Role;

import java.util.UUID;

public record RoleUpdatedEvent(
        UUID userId,
        Role previousRole,
        Role updatedRole
) implements NotificationContent {
    @Override
    public String title() {
        return "권한이 변경되었습니다.";
    }

    @Override
    public String content() {
        return String.format("%s -> %s", previousRole, updatedRole);
    }
}
