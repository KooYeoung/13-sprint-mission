package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.ReadStatus;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChannelParticipantMapper {

    private final UserMapper userMapper;

    @Named("readStatusesToParticipants")
    public List<UserDto> toParticipants(List<ReadStatus> readStatuses) {
        return readStatuses
                .stream()
                .map(ReadStatus::getUser)
                .map(userMapper::toDto)
                .toList();
    }
}
