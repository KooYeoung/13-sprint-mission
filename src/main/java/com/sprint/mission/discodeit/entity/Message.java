package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
import com.sprint.mission.discodeit.entity.base.UpdatableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "messages")
@ToString(exclude = {"messageFiles", "author", "channel"}, callSuper = true)
public class Message extends UpdatableEntity {

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @OneToMany(mappedBy = "message")
    private List<MessageFile> messageFiles = new ArrayList<>();

    @Builder
    public Message(User author, Channel channel, MessageCreateCommand command) {
        this.content = command.content();
        this.author = author;
        this.channel = channel;
    }

    public void updateInfo(MessageUpdateCommand command) {
        this.content = command.content();
    }

    public UUID getChannelId() {
        if (channel == null) return null;
        return channel.getId();
    }

}
