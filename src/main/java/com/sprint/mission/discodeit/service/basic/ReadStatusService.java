package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ReadStatusResponse;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReadStatusService {
   private final ReadStatusRepository readStatusRepository;
   private final UserRepository userRepository;
   private final ChannelRepository channelRepository;

   public ReadStatusResponse save(ReadStatusCreateRequest request) {
      validateChannelAndReadStatus(request.userId(), request.channelId());

      ReadStatus readStatus = request.toReadStatus();
      readStatusRepository.save(readStatus);

      return ReadStatusResponse.from(readStatus);
   }

   public ReadStatusResponse findById(UUID id) {
      ReadStatus readStatus = getReadStatusRequireThrow(id);
      return ReadStatusResponse.from(readStatus);
   }

   public List<ReadStatusResponse> findAllByUserId(UUID userId) {

      return readStatusRepository.findByUserId(userId)
            .stream()
            .map(ReadStatusResponse::from)
            .toList();

   }

   public ReadStatusResponse update(ReadStatusUpdateRequest request) {

      Optional<ReadStatus> optionalReadStatus = readStatusRepository.findById(request.id());
      ReadStatus result;
      if (optionalReadStatus.isPresent()) {
         ReadStatus currentStatus = optionalReadStatus.get();
         result = currentStatus.withUpdatedAt(request.readAt());
         readStatusRepository.update(result);
      } else {
         validateChannelAndReadStatus(request.userId(), request.channelId());

         result = new ReadStatus(
               request.readAt()
               , request.userId()
               , request.channelId());
         readStatusRepository.save(result);
      }

      return ReadStatusResponse.from(result);
   }

   public void delete(UUID id) {
      getReadStatusRequireThrow(id);
      readStatusRepository.delete(id);
   }

   private ReadStatus getReadStatusRequireThrow(UUID id) {
      return readStatusRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("읽음 상태가 존재 하지 않습니다."));
   }

   private Channel getChannelRequireThrow(UUID channelId) {
      return channelRepository.findById(channelId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채널입니다."));
   }

   private void getUserRequireThrow(UUID userId) {
      userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
   }

   private void validateChannelAndReadStatus(UUID userId, UUID channelId) {
      getUserRequireThrow(userId);

      Channel channel = getChannelRequireThrow(channelId);

      if (!channel.isPrivate()) throw new IllegalArgumentException("비공개 채널만 등록 가능합니다.");

      boolean hasReadStatus = readStatusRepository.findByUserId(userId).stream()
            .anyMatch(r -> r.getChannelId().equals(channelId));

      if (hasReadStatus) throw new IllegalArgumentException("이미 읽음 상태가 존재합니다.");
   }


}
