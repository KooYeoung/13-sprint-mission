package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.dto.response.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.notification.NotificationNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.NotificationMapper;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.basic.BasicNotificationService;
import com.sprint.mission.discodeit.service.basic.UserReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.sprint.mission.discodeit.support.NotificationTestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicNotificationServiceTest {
    @Mock NotificationRepository repository;
    @Mock NotificationMapper mapper;
    @Mock UserReader userReader;
    @InjectMocks BasicNotificationService service;
    @Captor ArgumentCaptor<List<Notification>> notifications;

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void createAll_buildsOneRealNotificationPerReceiver() {
        User first = identified(user());
        User second = identified(user());
        given(userReader.getReferenceUser(first.getId())).willReturn(first);
        given(userReader.getReferenceUser(second.getId())).willReturn(second);
        service.createAll(new NotificationCreateCommand(List.of(first.getId(), second.getId()), "title", "content"));

        verify(repository).saveAll(notifications.capture());
        assertThat(notifications.getValue()).extracting(Notification::getReceiverId).containsExactly(first.getId(), second.getId());
        assertThat(notifications.getValue()).allSatisfy(notification -> {
            assertThat(notification.getTitle()).isEqualTo("title");
            assertThat(notification.getContent()).isEqualTo("content");
        });
    }

    @Test
    void createAll_skipsStorageWhenRecipientsAreEmpty() {
        service.createAll(new NotificationCreateCommand(List.of(), "title", "content"));
        verifyNoInteractions(repository, userReader);
    }

    @Test
    void createAll_doesNotSaveWhenReceiverDoesNotExist() {
        UUID missing = UUID.randomUUID();
        given(userReader.getReferenceUser(missing)).willThrow(new UserNotFoundException(missing));
        assertThatThrownBy(() -> service.createAll(new NotificationCreateCommand(List.of(missing), "title", "content")))
                .isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void findAll_usesAuthenticatedUserId() {
        User receiver = identified(user());
        DiscodeitUserDetails principal = principal(receiver);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        Notification notification = Notification.of(new NotificationCreateCommand(List.of(receiver.getId()), "title", "content"), receiver);
        NotificationDto expected = new NotificationDto(UUID.randomUUID(), null, receiver.getId(), "title", "content");
        given(repository.findAllByReceiverId(receiver.getId())).willReturn(List.of(notification));
        given(mapper.toDto(notification)).willReturn(expected);

        assertThat(service.findAllForCurrentUser()).containsExactly(expected);
        verify(repository).findAllByReceiverId(receiver.getId());
    }

    @Test
    void delete_removesFoundNotification() {
        User receiver = identified(user());
        Notification notification = identified(Notification.of(new NotificationCreateCommand(List.of(receiver.getId()), "title", "content"), receiver));
        given(repository.findById(notification.getId())).willReturn(Optional.of(notification));
        service.delete(notification.getId());
        verify(repository).delete(notification);
    }

    @Test
    void delete_throwsWithoutDeletingWhenNotificationIsMissing() {
        UUID missing = UUID.randomUUID();
        given(repository.findById(missing)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(missing)).isInstanceOf(NotificationNotFoundException.class);
        verify(repository, never()).delete(any(Notification.class));
    }
}
