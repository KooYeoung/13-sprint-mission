package com.sprint.mission.discodeit.repository.impl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.QMessage;
import com.sprint.mission.discodeit.repository.MessageRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.sprint.mission.discodeit.entity.QChannel.channel;
import static com.sprint.mission.discodeit.entity.QMessage.message;
import static com.sprint.mission.discodeit.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryCustomImpl implements MessageRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<Message> findAllByChannelId(UUID channelId, Pageable pageable, UUID cursor) {

        int pageSize = pageable.getPageSize();

        List<Message> messages = jpaQueryFactory.select(message)
                .from(message)
                .join(message.channel, channel).fetchJoin()
                .leftJoin(message.author, user).fetchJoin()
                .leftJoin(user.profile).fetchJoin()
                .leftJoin(user.userStatus).fetchJoin()
                .where(
                        filterByChannelId(channelId),
                        filterByCursor(channelId, cursor)
                )
                .orderBy(message.createdAt.desc(), message.id.desc())
                .limit(pageSize + 1L)
                .fetch();

        boolean hasNext = messages.size() > pageSize;

        if (hasNext) {
            messages.remove(pageSize);
        }

        return new SliceImpl<>(messages, pageable, hasNext);
    }

    private BooleanExpression filterByCursor(UUID channelId, UUID cursor) {
        if (cursor == null) return null;

        QMessage cursorMessage = new QMessage("cursorMessage");
        var cursorCreatedAt = JPAExpressions
                .select(cursorMessage.createdAt)
                .from(cursorMessage)
                .where(
                        cursorMessage.id.eq(cursor),
                        cursorMessage.channel.id.eq(channelId)
                );

        return message.createdAt.lt(cursorCreatedAt)
                .or(
                        message.createdAt.eq(cursorCreatedAt)
                                .and(message.id.lt(cursor))
                );
    }

    private BooleanExpression filterByChannelId(UUID channelId) {
        if (channelId == null) throw new IllegalArgumentException("channelId must not be null");

        return channel.id.eq(channelId);
    }

}
