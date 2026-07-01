package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusUpdateCommand;
import com.sprint.mission.discodeit.entity.base.UpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_statuses")
@ToString(exclude = {"user"}, callSuper = true)
public class UserStatus extends UpdatableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    public UserStatus(User user, UserStatusCreateCommand command) {
        this.user = user;
        this.lastActiveAt = command.createdAt();
    }

    public void updateInfo(UserStatusUpdateCommand command) {
        this.lastActiveAt = command.updateAt();
    }

    // 마지막 접속 시간이 현재 시간으로부터 5분 이내이면 현재 접속 중인 유저
    public boolean isOnline() {
        if (lastActiveAt == null) return false;

        return lastActiveAt
                .plus(Duration.ofMinutes(5))
                .isAfter(Instant.now());
    }

    public UUID getUserId() {
        if (user == null) return null;

        return user.getId();
    }

}
