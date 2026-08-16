package com.sprint.mission.discodeit.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApiErrorResponse")
class ApiErrorResponseTest {

    @Test
    @DisplayName("collection details are copied defensively")
    void of_copiesCollectionDetailsDefensively() {
        // given
        List<String> messages = new ArrayList<>(List.of("first message"));

        // when
        ApiErrorResponse response = ApiErrorResponse.of(
                400,
                "TestException",
                "TEST_ERROR",
                "test error",
                Map.of("field", messages)
        );

        messages.add("second message");

        // then
        @SuppressWarnings("unchecked")
        List<String> copiedMessages = (List<String>) response.details().get("field");

        assertThat(copiedMessages).containsExactly("first message");
        assertThatThrownBy(() -> copiedMessages.add("third message"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
