package com.sprint.mission.discodeit.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
@With
public class Channel extends UpdatableEntity {
   private  final String channelName;
   private  final String description;
   private  final ChannelType channelType;

   @Builder
   public Channel(
         String channelName
         , String description
         , ChannelType channelType) {
      super(Instant.now());
      this.channelName = channelName;
      this.description = description;
      this.channelType = channelType;
   }

   private Channel(UUID id
         , Instant createdAt
         , Instant updatedAt
         , String channelName
         , String description
         , ChannelType channelType) {
      super(id, createdAt, updatedAt);
      this.channelName = channelName;
      this.description = description;
      this.channelType = channelType;
   }

   public Channel withUpdatedAt(Instant now){
      return new Channel(
            getId(),
            getCreatedAt(),
            now,
            channelName,
            description,
            channelType
      );
   }

   public boolean isPrivate() {
      return ChannelType.PRIVATE.equals(channelType);
   }
   public boolean isPublic() {
      return ChannelType.PUBLIC.equals(channelType);
   }

}
