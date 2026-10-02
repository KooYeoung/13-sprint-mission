package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.config.JpaAuditingTestConfig;
import com.sprint.mission.discodeit.config.QuerydslTestConfig;
import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@Import({JpaAuditingTestConfig.class, QuerydslTestConfig.class})
class NotificationRepositoryTest {
    @Autowired NotificationRepository repository;
    @Autowired UserRepository users;
    @Autowired EntityManager em;

    @Test
    void findAllByReceiverId_readsOnlyReceiversNotificationsFromDatabase() {
        User first = users.save(user());
        User second = users.save(user());
        User empty = users.save(user());
        Notification firstNotification = repository.save(notification(first, "first"));
        Notification secondNotification = repository.save(notification(first, "second"));
        repository.save(notification(second, "other"));
        em.flush();
        em.clear();

        assertThat(repository.findAllByReceiverId(first.getId())).extracting(Notification::getId)
                .containsExactlyInAnyOrder(firstNotification.getId(), secondNotification.getId());
        assertThat(repository.findAllByReceiverId(empty.getId())).isEmpty();
    }

    private Notification notification(User receiver, String title) {
        return Notification.of(new NotificationCreateCommand(List.of(receiver.getId()), title, "content"), receiver);
    }
}
