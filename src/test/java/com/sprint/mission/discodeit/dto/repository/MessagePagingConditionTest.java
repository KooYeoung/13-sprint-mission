package com.sprint.mission.discodeit.dto.repository;

import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.exception.message.MessageInvalidPagingConditionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessagePagingConditionTest {

    @Test
    @DisplayName("메시지 페이징 조건 생성 성공 - cursor는 첫 페이지 조회를 위해 null 허용")
    void constructor_allowsNullCursor_whenRequiredFieldsExist() {
        UUID channelId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        MessagePagingCondition condition = new MessagePagingCondition(channelId, pageable, null);

        assertThat(condition.channelId()).isEqualTo(channelId);
        assertThat(condition.pageable()).isEqualTo(pageable);
        assertThat(condition.cursor()).isNull();
    }

    @Test
    @DisplayName("메시지 페이징 조건 생성 실패 - channelId는 필수")
    void constructor_throwsMessageInvalidPagingConditionException_whenChannelIdIsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> new MessagePagingCondition(null, pageable, null))
                .isInstanceOfSatisfying(MessageInvalidPagingConditionException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MESSAGE_INVALID_PAGING_CONDITION);
                    assertThat(exception.getDetails()).containsEntry("parameter", "channelId");
                });
    }

    @Test
    @DisplayName("메시지 페이징 조건 생성 실패 - pageable은 필수")
    void constructor_throwsMessageInvalidPagingConditionException_whenPageableIsNull() {
        UUID channelId = UUID.randomUUID();

        assertThatThrownBy(() -> new MessagePagingCondition(channelId, null, null))
                .isInstanceOfSatisfying(MessageInvalidPagingConditionException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MESSAGE_INVALID_PAGING_CONDITION);
                    assertThat(exception.getDetails()).containsEntry("parameter", "pageable");
                });
    }
}
