package com.sprint.mission.discodeit.events;

import com.sprint.mission.discodeit.storage.BinaryContentUpload;

import java.util.UUID;

public record BinaryContentCreatedEvent(
        UUID binaryContentId,
        BinaryContentUpload contentUpload
) {
}
