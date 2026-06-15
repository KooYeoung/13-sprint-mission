package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.ReadStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
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

   public ReadStatusDto save(ReadStatusDto dto) {
      validateChannelAndReadStatus(dto.userId(), dto.channelId());

      ReadStatus readStatus = new ReadStatus(ReadStatusCreateCommand.from(dto));
      ReadStatus save = readStatusRepository.save(readStatus);

      return ReadStatusDto.from(save);
   }

   public ReadStatusDto findById(UUID id) {
      ReadStatus readStatus = getReadStatusRequireThrow(id);
      return ReadStatusDto.from(readStatus);
   }

   public List<ReadStatusDto> findAllByUserId(UUID userId) {

      return readStatusRepository.findByUserId(userId)
            .stream()
            .map(ReadStatusDto::from)
            .toList();

   }

   public ReadStatusDto update(ReadStatusDto dto) {

      Optional<ReadStatus> optionalReadStatus = readStatusRepository.findById(dto.id());
      ReadStatus result;
      if (optionalReadStatus.isPresent()) {
         ReadStatus currentStatus = optionalReadStatus.get();
         ReadStatus readStatus = currentStatus.updateInfo(ReadStatusUpdateCommand.from(dto));
         result = readStatusRepository.update(readStatus);
      } else {
         validateChannelAndReadStatus(dto.userId(), dto.channelId());

         ReadStatus readStatus = new ReadStatus(ReadStatusCreateCommand.from(dto));
         result = readStatusRepository.save(readStatus);
      }

      return ReadStatusDto.from(result);
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
