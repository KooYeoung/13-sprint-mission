package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.command.channel.ChannelCreateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelUpdateCommand;
import com.sprint.mission.discodeit.dto.response.ChannelDto;

import java.util.List;
import java.util.UUID;

public interface ChannelService {
    ChannelDto save(ChannelCreateCommand command);

    ChannelDto findById(UUID channelId);

    List<ChannelDto> findAllByUserId(UUID userId);

    ChannelDto update(UUID channelId, ChannelUpdateCommand command);

    void delete(UUID channelId);
}
