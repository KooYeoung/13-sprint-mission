package com.sprint.mission.discodeit.security;

import com.sprint.mission.discodeit.service.basic.ReadStatusReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReadStatusGuard {

    private final ReadStatusReader readStatusReader;

    public boolean isOwner(UUID readStatusId, UUID userId){
        return readStatusReader.getReadStatus(readStatusId).getUserId().equals(userId);
    }
}
