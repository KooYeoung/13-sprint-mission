package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;
import lombok.With;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String username,
        String email,
        @With BinaryContentDto profile,
        @With boolean online,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserDto from(User user) {

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                BinaryContentDto.from(user.getProfile()),
                user.isOnline(),
                RequestTimeZoneUtils.toOffsetDateTime(user.getCreatedAt()),
                RequestTimeZoneUtils.toOffsetDateTime(user.getUpdatedAt())
        );
    }


}
