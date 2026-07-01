package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePrivateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;
import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.exception.channel.ChannelError;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.channel.ChannelUpdateFailException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BasicChannelService implements ChannelService {
    private final ChannelRepository channelRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ReadStatusService readStatusService;
    private final MessageService messageService;


    @Override
    public ChannelDto save(ChannelCreateCommand command) {

        Channel channel = new Channel(command);
        Channel savedChannel = channelRepository.save(channel);

        if (command.isPrivate() && command instanceof ChannelCreatePrivateCommand privateCommand) {

            Instant now = Instant.now();
            privateCommand.participantIds()
                    .forEach(id -> readStatusService.save(savedChannel.getId(), new ReadStatusCreateCommand(id, now)));
        }

        return ChannelDto.from(savedChannel, null);
    }

    @Transactional(readOnly = true)
    @Override
    public ChannelDto findById(UUID channelId) {
        Channel channel = getChannelRequireThrow(channelId);

        Instant lastMessageAt = getLastMessageAt(channelId);

        return ChannelDto.from(channel, lastMessageAt);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChannelDto> findAllByUserId(UUID userId) {
        if (!userRepository.existsById(userId)) throw new UserNotFoundException();

        List<Channel> channels = channelRepository.findVisibleChannels(userId, ChannelType.PUBLIC);

        Map<UUID, Optional<Message>> channelLatestMessages = getLatestMessagesByChannels(channels);

        return channels
                .stream()
                .map(c -> ChannelDto.from(c, getLatestMessageTimestamp(c, channelLatestMessages)))
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChannelDto> findAll() {
        return channelRepository.findAll().stream()
                .map(ChannelDto::from)
                .toList();
    }

    @Override
    public ChannelDto update(UUID channelId, ChannelUpdateCommand command) {
        Channel channel = getChannelRequireThrow(channelId);

        if (channel.isPrivate()) throw new ChannelUpdateFailException(ChannelError.PRIVATE_NOT_UPDATE.getMessage());

        channel.updateInfo(command);

        return ChannelDto.from(channelRepository.save(channel), getLastMessageAt(channelId));
    }

    @Override
    public void delete(UUID channelId) {

        if (!channelRepository.existsById(channelId)) throw new ChannelNotFoundException();

        readStatusRepository.deleteByChannel_Id(channelId);

        messageService.deleteAllByChannelId(channelId);

        channelRepository.deleteById(channelId);

    }

    private Channel getChannelRequireThrow(UUID channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(ChannelNotFoundException::new);
    }

    private @Nullable Instant getLastMessageAt(UUID channelId) {
        return messageRepository.findTop1ByChannel_IdOrderByCreatedAtDesc(channelId)
                .map(Message::getCreatedAt)
                .orElse(null);
    }

    private @NonNull Map<UUID, Optional<Message>> getLatestMessagesByChannels(List<Channel> channels) {
        List<UUID> channelIds = channels
                .stream()
                .map(Channel::getId)
                .toList();

        return messageRepository.findLatestMessagesByChannelIds(channelIds)
                .stream()
                .collect(
                        Collectors.groupingBy(Message::getChannelId,
                                Collectors.maxBy(Comparator.comparing(Message::getCreatedAt))
                        )
                );
    }

    private @Nullable Instant getLatestMessageTimestamp(Channel c, Map<UUID, Optional<Message>> channelLatestMessages) {
        return channelLatestMessages.getOrDefault(c.getId(), Optional.empty())
                .map(Message::getCreatedAt)
                .orElse(null);
    }

}
