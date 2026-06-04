package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.ChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.ChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ChannelResponse;
import com.sprint.mission.discodeit.entity.BaseEntity;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasicChannelService implements ChannelService {
   private final ChannelRepository channelRepository;
   private final ReadStatusRepository readStatusRepository;
   private final MessageRepository messageRepository;


   @Override
   public ChannelResponse save(ChannelCreateRequest channelCreateRequest) {
      Channel channel = channelCreateRequest.toChannel();
      channelRepository.save(channel);

      return ChannelResponse.from(channel);
   }

   @Override
   public ChannelResponse findById(UUID channelId) {
      Channel channel = getChannelRequireThrow(channelId);

      ChannelResponse channelResponse = ChannelResponse.from(channel);

      if(channel.isPrivate()){
         List<UUID> userIds = readStatusRepository.findByChannelId(channelId)
               .stream()
               .map(ReadStatus::getUserId)
               .toList();

         channelResponse = channelResponse.withUserIds(userIds);

      }

      Optional<Message> max = messageRepository.findAllByChannelId(channelId)
            .stream().max(Comparator.comparing(BaseEntity::getCreatedAt));

      if(max.isPresent()) {
         channelResponse = channelResponse.withLastMessageAt(max.get().getCreatedAt());
      }

      return channelResponse;
   }

   @Override
   public List<ChannelResponse> findAllByUserId(UUID userId) {
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
               ChannelResponse channelResponse = ChannelResponse.from(c);
               if(c.isPrivate()) {
                  List<UUID> userIds = readStatusByChannelId.getOrDefault(c.getId(), new ArrayList<>());
                  channelResponse = channelResponse.withUserIds(userIds);
               }
               Optional<Message> optionalLatestMessage = channelLatestMessages.getOrDefault(c.getId(), Optional.empty());

               if(optionalLatestMessage.isPresent()) {
                  channelResponse = channelResponse.withLastMessageAt(optionalLatestMessage.get().getCreatedAt());
               }
               return channelResponse;
            })
            .toList();
   }

   @Override
   public List<ChannelResponse> findAll() {
      return channelRepository.findAll().stream()
            .map(ChannelResponse::from)
            .toList();
   }

   @Override
   public ChannelResponse update(ChannelUpdateRequest channelUpdateRequest) {
      Channel channel = getChannelRequireThrow(channelUpdateRequest.channelId());

      if(channel.isPrivate()) throw new IllegalArgumentException("PRIVATE 채널은 수정할 수 없습니다.");

      Channel updatedChannel = channel
            .withChannelName(channelUpdateRequest.isPrivate() ? "" :  channelUpdateRequest.channelName())
            .withDescription(channelUpdateRequest.isPrivate() ? "" : channelUpdateRequest.channelDescription())
            .withChannelType(channelUpdateRequest.channelType())
            .withUpdatedAt(Instant.now());

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
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채널 입니다."));
   }

}
