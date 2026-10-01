package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.entity.BinaryContentStatus;

import java.util.UUID;

public interface BinaryContentStatusUpdater {
    void updateStatus(UUID binaryContentId, BinaryContentStatus updatedStatus);
}
