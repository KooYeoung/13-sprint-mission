package com.sprint.mission.discodeit.repository.impl;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.MessageRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
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
    public Slice<Message> findAllByChannelId(UUID channelId, Pageable pageable, Instant cursor) {

        int pageSize = pageable.getPageSize();

        List<Message> messages = jpaQueryFactory.select(message)
                .from(message)
                .join(message.channel, channel).fetchJoin()
                .leftJoin(message.author, user).fetchJoin()
                .leftJoin(user.profile).fetchJoin()
                .leftJoin(user.userStatus).fetchJoin()
                .where(
                        filterByChannelId(channelId),
                        filterByCreatedAt(cursor)
                )
                .orderBy(message.createdAt.desc())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = messages.size() > pageSize;

        if(hasNext){
            messages.remove(pageSize);
        }

        return new SliceImpl<>(messages, pageable, hasNext);
    }

    private BooleanExpression filterByCreatedAt(Instant cursor) {
        if (cursor == null) return null;

        return message.createdAt.lt(cursor);
    }

    private BooleanExpression filterByChannelId(UUID channelId) {
        if (channelId == null) throw new IllegalArgumentException("channelId must not be null");

        return channel.id.eq(channelId);
    }

    // example..
    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return new OrderSpecifier[]{
                    message.createdAt.desc()
            };
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();

        for (Sort.Order sortOrder : pageable.getSort()) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

            switch (sortOrder.getProperty()) {
                case "createdAt" -> orders.add(new OrderSpecifier<>(direction, message.createdAt));
                case "updatedAt" -> orders.add(new OrderSpecifier<>(direction, message.updatedAt));
                default -> throw new IllegalArgumentException("지원하지 않는 정렬 필드입니다: " + sortOrder.getProperty());
            }
        }

        return orders.toArray(OrderSpecifier[]::new);
    }
}
