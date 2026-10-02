package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.notification.NotificationCreateCommand;
import com.sprint.mission.discodeit.dto.response.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.exception.notification.NotificationNotFoundException;
import com.sprint.mission.discodeit.mapper.NotificationMapper;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.security.DiscodeitUserDetails;
import com.sprint.mission.discodeit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class BasicNotificationService implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserReader userReader;

    @Override
    public void createAll(NotificationCreateCommand command) {
        List<Notification> createdNotifications = command.receiverIds()
                .stream()
                .filter(Objects::nonNull)
                .map(receiverId -> Notification.of(command, userReader.getReferenceUser(receiverId)))
                .toList();

        if (createdNotifications.isEmpty()) {
            return;
        }

        notificationRepository.saveAll(createdNotifications);
    }

    @Override
    @PreAuthorize("@notificationGuard.isOwner(#notificationId, authentication.principal.userDto.id)")
    public void delete(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notificationRepository.delete(notification);
    }


    @Transactional(readOnly = true)
    @Override
    public List<NotificationDto> findAllForCurrentUser() {
        DiscodeitUserDetails userDetails = (DiscodeitUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UUID userId = userDetails.getUserDto().id();

        return notificationRepository.findAllByReceiverId(userId)
                .stream()
                .map(notificationMapper::toDto)
                .toList();
    }
}
