package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.utils.RequestTimeZoneUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ChannelDto(
        UUID id,
        ChannelType type,
        String name,
        String description,
        List<UUID> participantIds,
        OffsetDateTime lastMessageAt
) {
   public static ChannelDto from(Channel channel) {
      return new ChannelDto(
              channel.getId(),
              channel.getChannelType(),
              channel.getChannelName(),
              channel.getDescription(),
              new ArrayList<>(),
              null
      );
   }

   public static ChannelDto from(Channel channel, Instant lastMessageAt, List<UUID> participantIds) {
      return new ChannelDto(
              channel.getId(),
              channel.getChannelType(),
              channel.getChannelName(),
              channel.getDescription(),
              participantIds,
              RequestTimeZoneUtils.toOffsetDateTime(lastMessageAt)
      );
   }

   public boolean isPrivate() {
      return  ChannelType.PRIVATE.equals(type);
   }
}