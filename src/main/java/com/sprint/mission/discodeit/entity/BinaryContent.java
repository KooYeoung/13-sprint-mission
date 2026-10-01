package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.UpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "binary_contents")
@ToString(callSuper = true)
public class BinaryContent extends UpdatableEntity {

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BinaryContentStatus status;

    private BinaryContent(String originalFileName, String contentType, Long size) {
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.size = size;
        this.status = BinaryContentStatus.PROCESSING;
    }

    public static BinaryContent createProcessing(String originalFileName, String contentType, Long size) {
        return new BinaryContent(originalFileName, contentType, size);
    }

    public void updateStatus(BinaryContentStatus status) {
        this.status = status;
    }


}
