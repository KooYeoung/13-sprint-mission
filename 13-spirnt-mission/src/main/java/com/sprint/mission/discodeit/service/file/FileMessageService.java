package com.sprint.mission.discodeit.service.file;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.file.FileChannelRepository;
import com.sprint.mission.discodeit.repository.file.FileMessageRepository;
import com.sprint.mission.discodeit.repository.file.FileUserRepository;
import com.sprint.mission.discodeit.service.MessageService;

import java.util.List;
import java.util.UUID;

public class FileMessageService implements MessageService {
   private final FileMessageRepository fileMessageRepository = new FileMessageRepository();
   private final UserRepository userRepository = new FileUserRepository();
   private final ChannelRepository channelRepository = new FileChannelRepository();

   @Override
   public void save(Message message) {
      if(isUserOrChannelMissing(message)) return;
      fileMessageRepository.save(message);
   }

   @Override
   public Message findById(UUID messageId) {
      return fileMessageRepository.findById(messageId);
   }

   @Override
   public List<Message> findAll() {
      return fileMessageRepository.findAll();
   }

   @Override
   public void update(UUID messageId, String content) {
      Message message = findById(messageId);
      if(message == null) return;
      if(isUserOrChannelMissing(message)) return;
      message.update(content);
      fileMessageRepository.save(message);
   }

   @Override
   public void delete(UUID messageId) {
      Message message = findById(messageId);
      if(message == null) return;
      if(isUserOrChannelMissing(message)) return;
      fileMessageRepository.delete(messageId);

   }

   private boolean isUserOrChannelMissing(Message message) {
      User user = userRepository.findById(message.getUser().getId());
      Channel byId = channelRepository.findById(message.getChannel().getId());
      return user == null || byId == null;
   }

}
