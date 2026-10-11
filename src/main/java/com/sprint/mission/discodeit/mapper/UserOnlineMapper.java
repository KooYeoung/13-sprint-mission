package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.service.basic.UserOnlineStatusReader;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserOnlineMapper {

    private final UserOnlineStatusReader userOnlineStatusReader;

    @Named("isOnline")
    public boolean isOnline(UUID userId) {
        return userOnlineStatusReader.isOnline(userId);
    }
}
