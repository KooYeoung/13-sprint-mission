package com.sprint.mission.discodeit.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("바이너리 파일 메타데이터 단위 테스트")
class BinaryContentTest {

    @Test
    @DisplayName("새 메타데이터는 업로드 중 상태와 전달한 파일 정보로 생성된다")
    void createProcessing_initializesMetadata() {
        BinaryContent content = BinaryContent.createProcessing("avatar.png", "image/png", 20L);

        assertThat(content.getStatus()).isEqualTo(BinaryContentStatus.PROCESSING);
        assertThat(content.getOriginalFileName()).isEqualTo("avatar.png");
        assertThat(content.getContentType()).isEqualTo("image/png");
        assertThat(content.getSize()).isEqualTo(20L);
    }

    @ParameterizedTest
    @EnumSource(value = BinaryContentStatus.class, names = {"SUCCESS", "FAIL"})
    @DisplayName("업로드 결과에 따라 실제 엔티티의 상태가 변경된다")
    void updateStatus_changesUploadStatus(BinaryContentStatus result) {
        BinaryContent content = BinaryContent.createProcessing("file.txt", "text/plain", 10L);

        content.updateStatus(result);

        assertThat(content.getStatus()).isEqualTo(result);
    }
}
