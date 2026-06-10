package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.entity.*;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasicMessageService implements MessageService {
   private final MessageRepository messageRepository;
   private final UserRepository userRepository;
   private final ChannelRepository channelRepository;
   private final BinaryContentRepository binaryContentRepository;


   @Override
   public MessageDto save(MessageDto messageDto, List<MultipartFile> files) {
      getChannelRequireThrow(messageDto.channelId());
      User user = getUserRequireThrow(messageDto.userId());

      MessageCreateCommand command = MessageCreateCommand.from(messageDto);
      if (files!=null && !files.isEmpty()) {
         List<UUID> attachedFileIds = new ArrayList<>();
         for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
               BinaryContent binaryContent = BinaryContent.builder()
                     .originalFileName(file.getOriginalFilename())
                     .contentType(file.getContentType())
                     .build();

               binaryContentRepository.save(binaryContent);
               attachedFileIds.add(binaryContent.getId());
            }
         }
         command = command.withFileIds(attachedFileIds);
      }

      Message message = new Message(command);

      messageRepository.save(message);

      return MessageDto.from(message, user.getNickname());
   }

   @Override
   public MessageDto findById(UUID messageId) {
      Message message = getMessageRequireThrow(messageId);

      User user = getUserRequireThrow(message.getUserId());
      getChannelRequireThrow(message.getChannelId());

      return MessageDto.from(message, user.getNickname());
   }

   @Override
   public List<MessageDto> findAll() {
      Map<UUID, User> userIdMap = userRepository.findAll()
            .stream()
            .collect(Collectors.toMap(User::getId, u -> u));

      return messageRepository.findAll()
            .stream()
            .filter(m -> userIdMap.get(m.getUserId()) != null)
            .map(m -> MessageDto.from(m, userIdMap.get(m.getUserId()).getNickname()))
            .toList();
   }

   @Override
   public List<MessageDto> findAllByChannelId(UUID channelId) {

      return findAll()
            .stream()
            .filter(m -> m.channelId().equals(channelId))
            .toList();
   }

   @Override
   public MessageDto update(MessageDto dto) {

      Message message = getMessageRequireThrow(dto.messageId());

      getChannelRequireThrow(message.getChannelId());
      getUserRequireThrow(message.getUserId());

      Message updatedMessage = message.updateInfo(MessageUpdateCommand.from(dto));

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
               .forEach(binaryContentRepository::delete);
      }
   }

   private Optional<Channel> getChannel(UUID channelId) {
      return channelRepository.findById(channelId);
   }

   private Channel getChannelRequireThrow(UUID channelId) {
      return getChannel(channelId)
            .orElseThrow(() -> new IllegalArgumentException("존재 하지 않는 채널 입니다."));
   }

   private Optional<User> getUser(UUID userId) {
      return userRepository.findById(userId);
   }

   private User getUserRequireThrow(UUID userId) {
      return getUser(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 입니다."));
   }

   private Message getMessageRequireThrow(UUID messageId) {
      return messageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메시지 입니다."));
   }
}
