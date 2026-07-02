package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;
import com.sprint.mission.discodeit.entity.base.UpdatableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "channels")
@ToString(exclude = {"readStatusList"}, callSuper = true)
public class Channel extends UpdatableEntity {

    @Column(length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChannelType type;

    @OneToMany(mappedBy = "channel", cascade = CascadeType.REMOVE)
    private List<ReadStatus> readStatusList = new ArrayList<>();

    @Builder
    public Channel(ChannelCreateCommand command) {
        this.name = command.channelName();
        this.description = command.channelDescription();
        this.type = command.channelType();
    }

    public void updateInfo(ChannelUpdateCommand command) {
        this.name = command.channelName();
        this.description = command.channelDescription();
    }

    public boolean isPrivate() {
        return ChannelType.PRIVATE.equals(type);
    }

    public boolean isPublic() {
        return ChannelType.PUBLIC.equals(type);
    }


}
