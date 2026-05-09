package com.sprint.mission.discodeit.entity;

import java.util.UUID;

public class Message extends BaseEntity{
   private String content;
   private User user;
   private Channel channel;

   public Message(String content, User user, Channel channel){
      this.content = content;
      this.user = user;
      this.channel = channel;
   }

   public void update(String content){
      super.update();
      this.content = content;
   }

   @Override
   public String toString() {
      return "Message{" +
            "id='" + getId() + '\'' +
            ", content='" + content + '\'' +
            ", user=" + user +
            ", channel=" + channel +
            ", createdAt='" + getCreatedAt() + '\'' +
            ", updatedAt='" + getUpdatedAt() + '\'' +
            '}';
   }

   public String getContent() {
      return content;
   }

   public User getUser() {
      return user;
   }
   public UUID getUserId() {
      return user.getId();
   }

   public Channel getChannel() {
      return channel;
   }
   public UUID getChannelId() {
      return channel.getId();
   }
}
