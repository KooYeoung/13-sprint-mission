package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusUpdateCommand;
import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.readStatus.ReadStatusBadRequestException;
import com.sprint.mission.discodeit.exception.readStatus.ReadStatusError;
import com.sprint.mission.discodeit.exception.readStatus.ReadStatusNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.ReadStatusMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReadStatusService {
    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ReadStatusMapper readStatusMapper;

    public ReadStatusDto save(UUID channelId, ReadStatusCreateCommand command) {
        User user = getUserRequireThrow(command.userId());
        Channel channel = validateChannelAndReadStatus(command.userId(), channelId);

        ReadStatus save = readStatusRepository.save(new ReadStatus(channel, user, command));

        return ReadStatusDto.from(save);
    }

    @Transactional(readOnly = true)
    public ReadStatusDto findById(UUID id) {
        ReadStatus readStatus = getReadStatusRequireThrow(id);
        return ReadStatusDto.from(readStatus);
    }

    @Transactional(readOnly = true)
    public List<ReadStatusDto> findAllByUserId(UUID userId) {

        return readStatusRepository.findByUser_Id(userId)
                .stream()
                .map(ReadStatusDto::from)
                .toList();

    }

    public ReadStatusDto update(UUID readStatusId, ReadStatusUpdateCommand command) {

        ReadStatus readStatus = getReadStatusRequireThrow(readStatusId);

        readStatus.updateInfo(command);

        return ReadStatusDto.from(readStatusRepository.save(readStatus));
    }

    public void delete(UUID id) {
        if (!readStatusRepository.existsById(id)) throw new ReadStatusNotFoundException();
        readStatusRepository.deleteById(id);
    }

    private ReadStatus getReadStatusRequireThrow(UUID id) {
        return readStatusRepository.findById(id)
                .orElseThrow(ReadStatusNotFoundException::new);
    }

    private Channel getChannelRequireThrow(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(ChannelNotFoundException::new);
    }

    private User getUserRequireThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private Channel validateChannelAndReadStatus(UUID userId, UUID channelId) {

        Channel channel = getChannelRequireThrow(channelId);

        boolean hasReadStatus = readStatusRepository.existsByChannel_IdAndUser_Id(channelId, userId);

        if (hasReadStatus) throw new ReadStatusBadRequestException(ReadStatusError.HAS_READ.getMessage());

        return channel;
    }


    public void saveAll(Channel channel, List<UUID> userIds, Instant readAt) {

        if (userIds == null) throw new UserNotFoundException();

        List<UUID> distinctUserIds = userIds.stream().distinct().toList();
        if(distinctUserIds.isEmpty()) throw new UserNotFoundException();

        int insertedRowsCount = readStatusRepository.burkInsert(channel.getId(), userIds, readAt);

        if(insertedRowsCount != distinctUserIds.size()) throw new UserNotFoundException();

    }

    @Transactional(readOnly = true)
    public List<ReadStatus> findAllByChannelId(UUID channelId) {
        return readStatusRepository.findByChannel_Id(channelId);
    }

    @Transactional(readOnly = true)
    public List<ReadStatus> findAllByChannelIds(List<UUID> channelIds) {
        return readStatusRepository.findByChannel_IdIn(channelIds);
    }

    public void deleteByChannelId(UUID channelId) {
        if(!readStatusRepository.exxistsByChannel_Id(channelId)) return;

        readStatusRepository.deleteByChannel_Id(channelId);
    }
}
