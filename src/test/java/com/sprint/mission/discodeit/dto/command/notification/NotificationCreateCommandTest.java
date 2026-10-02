package com.sprint.mission.discodeit.dto.command.notification;

import com.sprint.mission.discodeit.events.NotificationContent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationCreateCommandTest {
    @ParameterizedTest
    @ValueSource(ints = {254, 255, 256})
    void constructors_preserveOrTruncateTitleAndContentByCodePoint(int size) {
        String source = "😀".repeat(size);
        String expected = size <= 255 ? source : "😀".repeat(252) + "...";
        UUID receiver = UUID.randomUUID();
        NotificationContent content = content(source);

        for (NotificationCreateCommand command : List.of(
                new NotificationCreateCommand(receiver, content),
                new NotificationCreateCommand(List.of(receiver), content))) {
            assertThat(command.receiverIds()).containsExactly(receiver);
            assertThat(command.title()).isEqualTo(expected);
            assertThat(command.content()).isEqualTo(expected);
            assertThat(command.content().codePointCount(0, command.content().length())).isLessThanOrEqualTo(255);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "한"})
    void truncation_keepsEmojiWholeAtBoundary(String character) {
        String prefix = character.repeat(251) + "😀";
        NotificationCreateCommand command = new NotificationCreateCommand(UUID.randomUUID(), content(prefix + "뒤".repeat(10)));
        assertThat(command.title()).isEqualTo(prefix + "...");
        assertThat(command.content()).isEqualTo(prefix + "...");
    }

    private NotificationContent content(String text) {
        return new NotificationContent() {
            public String title() { return text; }
            public String content() { return text; }
        };
    }
}
