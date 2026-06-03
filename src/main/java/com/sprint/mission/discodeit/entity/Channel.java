package com.sprint.mission.discodeit.entity;

public class Channel extends BaseEntity{
   private String channelName;
   private String description;
   private ChannelType channelType;

   public Channel(String channelName, String description, ChannelType channelType){
      this.channelName = channelName;
      this.description = description;
      this.channelType = channelType;
   }

   public void update(String channelName, String description, ChannelType channelType){
      super.update();
      this.channelName = channelName;
      this.description = description;
      this.channelType = channelType;
   }

   @Override
   public String toString() {
      return "Channel{" +
            "id='" + getId() + '\'' +
            ", channelName='" + channelName + '\'' +
            ", description='" + description + '\'' +
            ", channelType=" + channelType +
            ", createdAt=" + getCreatedAt() +
            ", updatedAt=" + getUpdatedAt() +
            '}';
   }

   public String getChannelName() {
      return channelName;
   }

   public String getDescription() {
      return description;
   }

   public ChannelType getChannelType() {
      return channelType;
   }
}
