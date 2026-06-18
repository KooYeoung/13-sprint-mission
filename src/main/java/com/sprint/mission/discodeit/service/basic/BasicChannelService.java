package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.entity.BaseEntity;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.exception.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.ChannelUpdateFailException;
import com.sprint.mission.discodeit.exception.UserNotFoundException;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasicChannelService implements ChannelService {
   private final ChannelRepository channelRepository;
   private final ReadStatusRepository readStatusRepository;
   private final MessageRepository messageRepository;
   private final UserRepository userRepository;


   @Override
   public ChannelDto save(ChannelCreateCommand command) {

      Channel channel = new Channel(command);
      channelRepository.save(channel);

      return ChannelDto.from(channel);
   }

   @Override
   public ChannelDto findById(UUID channelId) {
      Channel channel = getChannelRequireThrow(channelId);

      ChannelDto channelDto = ChannelDto.from(channel);

      if(channel.isPrivate()){
         List<UUID> userIds = readStatusRepository.findByChannelId(channelId)
               .stream()
               .map(ReadStatus::getUserId)
               .toList();

         channelDto = channelDto.withUserIds(userIds);

      }

      Optional<Message> max = messageRepository.findAllByChannelId(channelId)
            .stream().max(Comparator.comparing(BaseEntity::getCreatedAt));

      if(max.isPresent()) {
         channelDto = channelDto.withLastMessageAt(max.get().getCreatedAt());
      }

      return channelDto;
   }

   @Override
   public List<ChannelDto> findAllByUserId(UUID userId) {
      userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

      List<UUID> channelIds = readStatusRepository.findByUserId(userId)
            .stream()
            .map(ReadStatus::getChannelId)
            .toList();

      Map<UUID, List<UUID>> readStatusByChannelId = readStatusRepository.findAll()
            .stream()
            .filter(r -> channelIds.contains(r.getChannelId()))
            .collect(Collectors.groupingBy(ReadStatus::getChannelId
                  ,Collectors.mapping(ReadStatus::getUserId,Collectors.toList())));

      Map<UUID, Optional<Message>> channelLatestMessages = messageRepository.findAll()
            .stream()
            .filter(m -> channelIds.contains(m.getChannelId()))
            .collect(Collectors.groupingBy(Message::getChannelId, Collectors.maxBy(Comparator.comparing(Message::getCreatedAt))));

      return channelRepository.findAll()
            .stream()
            .filter(c -> c.isPrivate() && channelIds.contains(c.getId()) || c.isPublic())
            .map(c ->{
               ChannelDto channelDto = ChannelDto.from(c);
               if(c.isPrivate()) {
                  List<UUID> userIds = readStatusByChannelId.getOrDefault(c.getId(), new ArrayList<>());
                  channelDto = channelDto.withUserIds(userIds);
               }
               Optional<Message> optionalLatestMessage = channelLatestMessages.getOrDefault(c.getId(), Optional.empty());

               if(optionalLatestMessage.isPresent()) {
                  channelDto = channelDto.withLastMessageAt(optionalLatestMessage.get().getCreatedAt());
               }
               return channelDto;
            })
            .toList();
   }

   @Override
   public List<ChannelDto> findAll() {
      return channelRepository.findAll().stream()
            .map(ChannelDto::from)
            .toList();
   }

   @Override
   public ChannelDto update(UUID channelId,  ChannelUpdateCommand command) {
      Channel channel = getChannelRequireThrow(channelId);

      if(channel.isPrivate()) throw new ChannelUpdateFailException("PRIVATE 채널은 수정할 수 없습니다.");

      Channel updatedChannel = channel.updateInfo(command);

      channelRepository.save(updatedChannel);

      return findById(updatedChannel.getId());
   }

   @Override
   public void delete(UUID channelId) {
      Channel channel = getChannelRequireThrow(channelId);

      channelRepository.delete(channel.getId());
      readStatusRepository.deleteByChannelId(channelId);
      messageRepository.deleteAllByChannelId(channelId);

   }

   private Channel getChannelRequireThrow(UUID channelId) {
      return channelRepository.findById(channelId)
            .orElseThrow(ChannelNotFoundException::new);
   }

}
