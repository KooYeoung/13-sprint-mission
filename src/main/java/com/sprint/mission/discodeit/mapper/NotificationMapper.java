package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.NotificationDto;
import com.sprint.mission.discodeit.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        config = MapStructConfig.class,
        uses = {DateTimeMapper.class}
)
public interface NotificationMapper {

    @Mapping(source = "notification.id", target = "id")
    @Mapping(source = "notification.receiverId", target = "receiverId")
    @Mapping(source = "notification.title", target = "title")
    @Mapping(source = "notification.content", target = "content")
    @Mapping(source = "notification.createdAt", target = "createdAt", qualifiedByName = "toOffsetDateTime")
    NotificationDto toDto(Notification notification);
}
