package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("바이너리 파일 상태 변경 서비스 단위 테스트")
class TransactionalBinaryContentStatusUpdaterTest {
    @Mock BinaryContentRepository repository;
    @InjectMocks TransactionalBinaryContentStatusUpdater updater;

    @ParameterizedTest
    @EnumSource(value = BinaryContentStatus.class, names = {"SUCCESS", "FAIL"})
    @DisplayName("조회한 실제 엔티티에 업로드 결과를 반영한다")
    void updateStatus_changesEntityState(BinaryContentStatus targetStatus) {
        UUID id = UUID.randomUUID();
        BinaryContent content = BinaryContent.createProcessing("file.txt", "text/plain", 1L);
        given(repository.findById(id)).willReturn(Optional.of(content));

        updater.updateStatus(id, targetStatus);

        assertThat(content.getStatus()).isEqualTo(targetStatus);
    }

    @Test
    @DisplayName("메타데이터가 없으면 새로 저장하지 않고 상태 변경을 생략한다")
    void updateStatus_skipsMissingMetadata() {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        assertThatCode(() -> updater.updateStatus(id, BinaryContentStatus.FAIL)).doesNotThrowAnyException();

        verify(repository).findById(id);
        verify(repository, never()).save(any());
    }
}
