package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePublicCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Getter
@ToString
public class Channel extends UpdatableEntity {
   private  final String channelName;
   private  final String description;
   private  final ChannelType channelType;

   @Builder
   public Channel(ChannelCreateCommand command) {
      super(Instant.now());
      this.channelName = command.channelName();
      this.description = command.channelDescription();
      this.channelType = command.channelType();
   }

   public Channel updateInfo(ChannelUpdateCommand command){
      return new Channel(
              getId()
              ,getCreatedAt()
              ,Instant.now()
              , command.channelName()
              , command.channelDescription()
              , getChannelType()
      );
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

   public boolean isPrivate() {
      return ChannelType.PRIVATE.equals(channelType);
   }
   public boolean isPublic() {
      return ChannelType.PUBLIC.equals(channelType);
   }

}
