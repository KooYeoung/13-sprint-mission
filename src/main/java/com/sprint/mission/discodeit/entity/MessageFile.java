package com.sprint.mission.discodeit.entity;


import com.sprint.mission.discodeit.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "message_files",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "file_id"})
)
@ToString(exclude = {"message", "binaryContent"}, callSuper = true)
public class MessageFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private BinaryContent binaryContent;

    public MessageFile(Message message, BinaryContent binaryContent) {
        this.message = message;
        this.binaryContent = binaryContent;
    }
}
