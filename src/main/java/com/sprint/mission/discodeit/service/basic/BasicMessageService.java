package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.MessageNotFoundException;
import com.sprint.mission.discodeit.exception.UserNotFoundException;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BasicMessageService implements MessageService {
   private final MessageRepository messageRepository;
   private final UserRepository userRepository;
   private final ChannelRepository channelRepository;
   private final BinaryContentService binaryContentService;

   @Override
   public MessageDto save(MessageCreateCommand command, List<MultipartFile> files) {
      getChannelRequireThrow(command.channelId());
      getUserRequireThrow(command.userId());

      List<UUID> attachedFileIds = new ArrayList<>();
      if (files!=null && !files.isEmpty()) {
         for (MultipartFile file : files) {
            Optional<BinaryContentDto> binaryContentDto = binaryContentService.create(file);
            if(binaryContentDto.isEmpty()){
               continue;
            }
            BinaryContentDto savedBinaryContent = binaryContentDto.get();
            attachedFileIds.add(savedBinaryContent.id());

         }
      }

      Message message = new Message(command, attachedFileIds);

      messageRepository.save(message);

      return MessageDto.from(message);
   }

   @Override
   public MessageDto findById(UUID messageId) {
      Message message = getMessageRequireThrow(messageId);

      getUserRequireThrow(message.getUserId());
      getChannelRequireThrow(message.getChannelId());

      return MessageDto.from(message);
   }

   @Override
   public List<MessageDto> findAll() {

      return messageRepository.findAll()
            .stream()
            .map(MessageDto::from)
            .toList();
   }

   @Override
   public List<MessageDto> findAllByChannelId(UUID channelId) {
      getChannelRequireThrow(channelId);

      return findAll()
            .stream()
            .filter(m -> m.channelId().equals(channelId))
            .toList();
   }

   @Override
   public MessageDto update(UUID messageId,MessageUpdateCommand command) {

      Message message = getMessageRequireThrow(messageId);

      getChannelRequireThrow(message.getChannelId());
      getUserRequireThrow(message.getUserId());

      Message updatedMessage = message.updateInfo(command);

      messageRepository.save(updatedMessage);

      return findById(updatedMessage.getId());
   }

   @Override
   public void delete(UUID messageId) {
      Message message = getMessageRequireThrow(messageId);

      getChannelRequireThrow(message.getChannelId());
      getUserRequireThrow(message.getUserId());

      messageRepository.delete(messageId);
      if (!message.getFileIds().isEmpty()) {
         message.getFileIds()
               .forEach(binaryContentService::delete);
      }
   }

   private Optional<Channel> getChannel(UUID channelId) {
      return channelRepository.findById(channelId);
   }

   private Channel getChannelRequireThrow(UUID channelId) {
      return getChannel(channelId)
            .orElseThrow(ChannelNotFoundException::new);
   }

   private Optional<User> getUser(UUID userId) {
      return userRepository.findById(userId);
   }

   private User getUserRequireThrow(UUID userId) {
      return getUser(userId)
            .orElseThrow(UserNotFoundException::new);
   }

   private Message getMessageRequireThrow(UUID messageId) {
      return messageRepository.findById(messageId)
            .orElseThrow(MessageNotFoundException::new);
   }
}
