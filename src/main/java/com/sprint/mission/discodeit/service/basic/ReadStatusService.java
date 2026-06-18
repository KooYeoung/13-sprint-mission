package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.exception.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.ReadStatusBadRequestException;
import com.sprint.mission.discodeit.exception.ReadStatusNotFoundException;
import com.sprint.mission.discodeit.exception.UserNotFoundException;
import com.sprint.mission.discodeit.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadStatusService {
   private final ReadStatusRepository readStatusRepository;
   private final UserRepository userRepository;
   private final ChannelRepository channelRepository;

   public ReadStatusDto save(UUID channelId,ReadStatusCreateCommand command) {
      validateChannelAndReadStatus(command.userId(),channelId);

      ReadStatus readStatus = new ReadStatus(channelId, command);
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

   public ReadStatusDto update(UUID readStatusId,UUID userId, UUID channelId, ReadStatusUpdateCommand command) {

      Optional<ReadStatus> optionalReadStatus = readStatusRepository.findById(readStatusId);
      if(optionalReadStatus.isEmpty()){
         return save(channelId ,new ReadStatusCreateCommand(userId, command.readAt()));
      }

      ReadStatus currentStatus = optionalReadStatus.get();
      ReadStatus readStatus = currentStatus.updateInfo(command);
      readStatus = readStatusRepository.update(readStatus);

      return ReadStatusDto.from(readStatus);
   }

   public void delete(UUID id) {
      getReadStatusRequireThrow(id);
      readStatusRepository.delete(id);
   }

   private ReadStatus getReadStatusRequireThrow(UUID id) {
      return readStatusRepository.findById(id).orElseThrow(ReadStatusNotFoundException::new);
   }

   private Channel getChannelRequireThrow(UUID channelId) {
      return channelRepository.findById(channelId)
            .orElseThrow(ChannelNotFoundException::new);
   }

   private void getUserRequireThrow(UUID userId) {
      userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
   }

   private void validateChannelAndReadStatus(UUID userId, UUID channelId) {
      getUserRequireThrow(userId);

      Channel channel = getChannelRequireThrow(channelId);

      if (!channel.isPrivate()) throw new ReadStatusBadRequestException("비공개 채널만 등록 가능합니다.");

      boolean hasReadStatus = readStatusRepository.findByUserId(userId).stream()
            .anyMatch(r -> r.getChannelId().equals(channelId));

      if (hasReadStatus) throw new ReadStatusBadRequestException("이미 읽음 상태가 존재합니다.");
   }


}
