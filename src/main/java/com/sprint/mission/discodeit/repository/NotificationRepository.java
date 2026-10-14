package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Query("""
        SELECT n
        FROM Notification n
        WHERE n.receiver.id = :receiverId
    """)
    List<Notification> findAllByReceiverId(@Param("receiverId") UUID receiverId);
}
