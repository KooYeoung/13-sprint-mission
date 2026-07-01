package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageUpdateCommand;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.MessageFile;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.message.MessageNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BasicMessageService implements MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final MessageFileService messageFileService;

    @Override
    public MessageDto save(MessageCreateCommand command, List<MultipartFile> files) {
        Channel channel = getChannelRequireThrow(command.channelId());
        User author = getUserRequireThrow(command.userId());

        Message savedMessage = messageRepository.save(new Message(author, channel, command));
        List<MessageFile> savedMessageFiles = messageFileService.save(savedMessage, files);

        return MessageDto.from(savedMessage, savedMessageFiles);
    }

    @Transactional(readOnly = true)
    @Override
    public MessageDto findById(UUID messageId) {
        Message message = getMessageRequireThrow(messageId);

        return MessageDto.from(message);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MessageDto> findAll() {

        return messageRepository.findAll()
                .stream()
                .map(MessageDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<MessageDto> findAllByChannelId(UUID channelId) {

        return messageRepository.findAllByChannel_Id(channelId)
                .stream()
                .map(MessageDto::from)
                .toList();
    }

    @Override
    public MessageDto update(UUID messageId, MessageUpdateCommand command) {

        Message message = getMessageRequireThrow(messageId);

        message.updateInfo(command);

        return MessageDto.from(messageRepository.save(message));
    }

    @Override
    public void delete(UUID messageId) {
        if (!messageRepository.existsById(messageId)) throw new MessageNotFoundException();

        messageFileService.deleteByMessageId(messageId);

        messageRepository.deleteById(messageId);

    }

    @Override
    public void deleteAllByChannelId(UUID channelId) {
        messageRepository.findAllByChannel_Id(channelId)
                .forEach(message -> messageFileService.deleteAll(message.getMessageFiles()));

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
