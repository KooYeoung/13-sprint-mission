package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentStatusUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TransactionalBinaryContentStatusUpdater implements BinaryContentStatusUpdater {

    private final BinaryContentRepository binaryContentRepository;

    @Override
    public void updateStatus(UUID binaryContentId, BinaryContentStatus targetStatus) {
        binaryContentRepository.findById(binaryContentId)
                .ifPresentOrElse(
                        content -> updateBinaryContentStatus(targetStatus, content),
                        () -> log.warn("메타데이터가 없어 업로드 상태를 변경할 수 없습니다. binaryContentId={}, targetStatus={}",
                                binaryContentId, targetStatus)
                );
    }

    private void updateBinaryContentStatus(BinaryContentStatus targetStatus, BinaryContent content) {
        BinaryContentStatus previousStatus = content.getStatus();
        content.updateStatus(targetStatus);
        log.debug("바이너리 파일 업로드 상태를 변경했습니다. binaryContentId={}, previousStatus={}, targetStatus={}",
                content.getId(), previousStatus, targetStatus);
    }
}
