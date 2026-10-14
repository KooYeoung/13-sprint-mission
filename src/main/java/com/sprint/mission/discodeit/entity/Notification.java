package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"receiver"}, callSuper = true)
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    private String title;

    private String content;

    public UUID getReceiverId() {
        if (receiver == null) return null;
        return receiver.getId();
    }

    private Notification(User receiver, String title, String content) {
        this.receiver = receiver;
        this.title = title;
        this.content = content;
    }

    public static Notification of(NotificationCreateCommand command, User receiver) {
       return new Notification(receiver, command.title(), command.content());
    }
}
