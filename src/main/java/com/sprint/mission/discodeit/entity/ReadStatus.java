package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import com.sprint.mission.discodeit.entity.base.UpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "read_statuses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "channel_id"})
)
@ToString(exclude = {"user", "channel"}, callSuper = true)
public class ReadStatus extends UpdatableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    public ReadStatus(Channel channel, User user, ReadStatusCreateCommand command) {
        this.user = user;
        this.channel = channel;
        this.lastReadAt = command.readAt();
    }

    public void updateInfo(ReadStatusUpdateCommand command) {
        this.lastReadAt = command.readAt();
    }

    public UUID getUserId() {
        if (user == null) return null;
        return user.getId();
    }

    public UUID getChannelId() {
        if (channel == null) return null;
        return channel.getId();
    }

}
