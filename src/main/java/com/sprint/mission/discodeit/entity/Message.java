package com.sprint.mission.discodeit.entity;

import java.util.UUID;

public class Message extends BaseEntity{
   private String content;
   private UUID userId;
   private UUID channelId;
   private transient User user;
   private transient Channel channel;

   public Message(String content, User user, Channel channel){
      this.content = content;
      this.user = user;
      this.userId = user.getId();
      this.channel = channel;
      this.channelId = channel.getId();
   }

   public void update(String content){
      super.update();
      this.content = content;
   }

   public void attach( User user, Channel channel){
      this.user = user;
      this.channel = channel;
   }

   @Override
   public String toString() {
      return "Message{" +
            "content='" + content + '\'' +
            ", userId=" + userId +
            ", channelId=" + channelId +
            ", user=" + user +
            ", channel=" + channel +
            '}';
   }

   public String getContent() {
      return content;
   }

   public User getUser() {
      return user;
   }
   public UUID getUserId() {
      return userId;
   }

   public Channel getChannel() {
      return channel;
   }
   public UUID getChannelId() {
      return channelId;
   }
}
