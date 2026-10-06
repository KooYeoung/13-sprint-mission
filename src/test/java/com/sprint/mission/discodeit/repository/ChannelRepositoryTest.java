package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.config.JpaAuditingTestConfig;
import com.sprint.mission.discodeit.config.P6SpySqlFormatter;
import com.sprint.mission.discodeit.config.QuerydslTestConfig;
import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePrivateCommand;
import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePublicCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.readStatus.ReadStatusCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.repository.ChannelSummary;
import com.sprint.mission.discodeit.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest(showSql = false)
@Import(value = {QuerydslTestConfig.class, JpaAuditingTestConfig.class, P6SpySqlFormatter.class})
@DisplayName("ChannelRepository 슬라이스 테스트")
class ChannelRepositoryTest {

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    ReadStatusRepository readStatusRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("여러 채널이 있어도 지정한 공개 채널은 참여 기록 없이 접근할 수 있다")
    void isChannelAccessible_allowsPublicChannelWithoutMembership() {
        User requester = saveUser("accessRequester");
        Channel target = savePublicChannel().channel();
        savePublicChannel();
        Channel participating = savePrivateChannelFor(requester).channel();
        joinChannel(participating, requester);
        em.flush();
        em.clear();

        assertThat(channelRepository.isChannelAccessible(target.getId(), requester.getId())).isTrue();
    }

    @Test
    @DisplayName("비공개 채널은 대상 채널의 참여 기록으로만 접근을 판단한다")
    void isChannelAccessible_requiresMembershipInTheRequestedPrivateChannel() {
        User requester = saveUser("accessRequester");
        User other = saveUser("accessOther");
        Channel participating = savePrivateChannelFor(requester).channel();
        joinChannel(participating, requester);
        Channel target = savePrivateChannelFor(other).channel();
        joinChannel(target, other);
        savePublicChannel();
        em.flush();
        em.clear();

        assertThat(channelRepository.isChannelAccessible(participating.getId(), requester.getId())).isTrue();
        assertThat(channelRepository.isChannelAccessible(target.getId(), requester.getId())).isFalse();
        assertThat(channelRepository.isChannelAccessible(target.getId(), other.getId())).isTrue();
        assertThat(channelRepository.isChannelAccessible(UUID.randomUUID(), requester.getId())).isFalse();
    }

    @Test
    @DisplayName("채널이 없으면 접근 결과는 false다")
    void isChannelAccessible_returnsFalseWhenNoChannelsExist() {
        assertThat(channelRepository.isChannelAccessible(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("공개·비공개 타입을 요청하면 모든 채널 ID를 중복 없이 반환한다")
    void findVisibleChannelIds_returnsAllChannelsWhenAllTypesAreRequested() {
        User requester = saveUser("requester");
        User other = saveUser("other");
        Channel publicChannel = savePublicChannel().channel();
        Channel participating = savePrivateChannelFor(requester).channel();
        joinChannel(participating, requester);
        joinChannel(participating, other);
        Channel otherPrivate = savePrivateChannelFor(other).channel();
        joinChannel(otherPrivate, other);
        Channel withoutParticipants = savePrivateChannelFor(other).channel();
        em.clear();

        assertThat(channelRepository.findVisibleChannelIds(requester.getId(), List.of(ChannelType.PUBLIC, ChannelType.PRIVATE)))
                .containsExactlyInAnyOrder(publicChannel.getId(), participating.getId(), otherPrivate.getId(), withoutParticipants.getId());
    }

    @Test
    @DisplayName("일반 사용자에게 공개 채널과 참여 중인 비공개 채널 ID만 반환한다")
    void findVisibleChannelIds_returnsPublicAndParticipatingPrivateChannels() {
        User requester = saveUser("requester");
        User other = saveUser("other");
        Channel publicChannel = savePublicChannel().channel();
        Channel participating = savePrivateChannelFor(requester).channel();
        joinChannel(participating, requester);
        Channel otherPrivate = savePrivateChannelFor(other).channel();
        joinChannel(otherPrivate, other);
        em.clear();

        assertThat(channelRepository.findVisibleChannelIds(requester.getId(), List.of(ChannelType.PUBLIC)))
                .containsExactlyInAnyOrder(publicChannel.getId(), participating.getId());
    }

    @Test
    @DisplayName("비공개 채널 참여 여부는 생성 명령이 아니라 실제 읽음 상태로 판단한다")
    void findVisibleChannelIds_excludesPrivateChannelWithoutMembership() {
        User requester = saveUser("requester");
        savePrivateChannelFor(requester);
        Channel publicChannel = savePublicChannel().channel();
        em.clear();

        assertThat(channelRepository.findVisibleChannelIds(requester.getId(), List.of(ChannelType.PUBLIC)))
                .containsExactly(publicChannel.getId());
    }

    @Test
    void findVisibleChannelIds_returnsEmptyWhenNoChannelIsVisible() {
        User requester = saveUser("requester");
        savePrivateChannelFor(requester);
        em.clear();

        assertThat(channelRepository.findVisibleChannelIds(requester.getId(), List.of(ChannelType.PUBLIC))).isEmpty();
    }

    @Test
    @DisplayName("채널 상세 조회 성공 - 메시지가 있으면 마지막 메시지 시간을 포함한 ChannelSummary 반환")
    void findByDetail_returnsChannelSummaryWithLastMessageAt_whenChannelExistsAndMessagesExist() throws InterruptedException {
        // given
        // 이 테스트의 대상은 ChannelRepository.findByDetail(...) JPQL 쿼리다.
        // findByDetail(...)은 channelId 하나로 채널 상세 projection을 조회하고,
        // Message를 left join 한 뒤 max(m.createdAt)을 ChannelSummary.lastMessageAt으로 반환한다.
        //
        // 따라서 같은 채널에 메시지 2개를 저장한 뒤, 더 늦게 생성된 메시지 시간이
        // lastMessageAt으로 계산되는지 실제 DB 조회 결과로 검증한다.
        // 메시지를 저장하려면 작성자 User가 필요하다.
        // findByDetail(...) 자체는 사용자 가시성 조건을 보지 않지만, Message 엔티티의 author를 채우기 위한 데이터다.
        User user = saveUser("testUser");

        // 상세 조회 대상 채널이다.
        // PUBLIC/PRIVATE 여부와 관계없이 findByDetail(...)은 channelId가 일치하는 채널만 조회한다.
        PublicChannelFixture publicChannelFixture = savePublicChannel();
        ChannelCreatePublicCommand createPublicCommand = publicChannelFixture.command();
        Channel publicChannel = publicChannelFixture.channel();

        UUID userId = user.getId();
        UUID publicChannelId = publicChannel.getId();

        assertThat(userId).isNotNull();
        assertThat(publicChannelId).isNotNull();

        // 같은 채널에 메시지를 2개 저장한다.
        // createdAt은 JPA Auditing이 저장 시점에 채우므로, 두 메시지의 생성 시각이 명확히 달라지도록 짧게 대기한다.
        // 이 데이터가 있어야 max(m.createdAt)이 최신 메시지를 선택했다는 점을 검증할 수 있다.
        Message firstMessage = saveMessage(user, publicChannel, "messageContent1");
        Instant firstMessageCreatedAt = firstMessage.getCreatedAt();

        Thread.sleep(100);

        Message latestMessage = saveMessage(user, publicChannel, "messageContent2");
        Instant latestMessageCreatedAt = latestMessage.getCreatedAt();

        // 사전 조건을 확인한다.
        // 두 메시지가 같은 채널에 저장되었고, 두 번째 메시지가 실제로 더 늦게 생성되어야
        // lastMessageAt이 최신 메시지 시각인지 검증할 수 있다.
        assertThat(firstMessage.getId()).isNotNull();
        assertThat(latestMessage.getId()).isNotNull();
        assertThat(latestMessage.getId()).isNotEqualTo(firstMessage.getId());
        assertThat(firstMessage.getChannelId()).isEqualTo(publicChannelId);
        assertThat(latestMessage.getChannelId()).isEqualTo(publicChannelId);
        assertThat(firstMessageCreatedAt).isNotNull();
        assertThat(latestMessageCreatedAt).isNotNull();
        assertThat(latestMessageCreatedAt).isAfter(firstMessageCreatedAt);

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있던 Channel, Message 객체가 결과에 영향을 줄 수 없다.
        // 아래 when 절의 foundChannelSummary는 실제 JPQL SELECT, left join, group by, max 집계 결과다.
        em.clear();

        // when
        ChannelSummary foundChannelSummary = channelRepository.findByDetail(publicChannelId).orElseThrow(AssertionError::new);

        // then
        // foundChannelSummary는 단건 객체다.
        // 단건 객체에 extracting(...).containsExactlyInAnyOrder(tuple(...))를 쓰면
        // Iterable projection 검증처럼 동작하지 않아 assertion이 실패할 수 있다.
        // 단건 projection은 각 필드를 직접 검증하는 방식이 가장 명확하다.
        assertThat(foundChannelSummary)
                .satisfies(channelSummary -> {
                    assertThat(channelSummary.id()).isEqualTo(publicChannelId);
                    assertThat(channelSummary.type()).isEqualTo(ChannelType.PUBLIC);
                    assertThat(channelSummary.name()).isEqualTo(createPublicCommand.channelName());
                    assertThat(channelSummary.description()).isEqualTo(createPublicCommand.channelDescription());

                    // H2/JDBC가 timestamp를 저장하고 다시 읽는 과정에서 나노초 일부가 반올림될 수 있다.
                    // 그래서 저장 직후 엔티티의 Instant와 정확히 같은지보다,
                    // 최신 메시지 시각에 충분히 가깝고 첫 번째 메시지보다는 늦은지를 검증한다.
                    assertThat(channelSummary.lastMessageAt()).isNotNull();
                    assertThat(channelSummary.lastMessageAt()).isAfter(firstMessageCreatedAt);
                    assertThat(channelSummary.lastMessageAt())
                            .isAfterOrEqualTo(latestMessageCreatedAt.minusMillis(1))
                            .isBeforeOrEqualTo(latestMessageCreatedAt.plusMillis(1));
                });
    }

    @Test
    @DisplayName("채널 상세 조회 성공 - 메시지가 없으면 마지막 메시지 시간은 null")
    void findByDetail_returnsChannelSummaryWithNullLastMessageAt_whenChannelExistsAndMessagesDoNotExist() {
        // given
        // 이 테스트는 findByDetail(...)이 메시지가 없는 채널도 상세 조회 결과로 반환하는지 검증한다.
        // 쿼리가 Message를 left join 하므로, 연결된 Message row가 없어도 ChannelSummary는 반환되어야 하고
        // max(m.createdAt)의 결과인 lastMessageAt만 null이어야 한다.
        //
        // findByDetail(...)은 channelId만 조건으로 사용한다.
        // 메시지를 만들지 않는 이 케이스에서는 User나 ReadStatus가 필요하지 않다.
        PublicChannelFixture publicChannelFixture = savePublicChannel();
        ChannelCreatePublicCommand createPublicCommand = publicChannelFixture.command();
        Channel publicChannel = publicChannelFixture.channel();

        UUID publicChannelId = publicChannel.getId();

        // 사전 조건을 확인한다.
        // 채널은 실제 DB에 저장되어 있어야 하고, Message는 하나도 없어야 한다.
        assertThat(publicChannelId).isNotNull();
        assertThat(messageRepository.count()).isZero();

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있던 Channel 객체가 결과에 영향을 줄 수 없다.
        // 아래 when 절의 foundChannelSummary는 실제 JPQL SELECT, left join, group by, max 집계 결과다.
        em.clear();

        // when
        ChannelSummary foundChannelSummary = channelRepository.findByDetail(publicChannelId).orElseThrow(AssertionError::new);

        // then
        // 단건 projection이므로 tuple을 이용한 Iterable 스타일 검증 대신 필드를 직접 확인한다.
        // 메시지가 없더라도 left join 덕분에 채널 정보는 반환되고, lastMessageAt만 null이어야 한다.
        assertThat(foundChannelSummary)
                .satisfies(channelSummary -> {
                    assertThat(channelSummary.id()).isEqualTo(publicChannelId);
                    assertThat(channelSummary.type()).isEqualTo(ChannelType.PUBLIC);
                    assertThat(channelSummary.name()).isEqualTo(createPublicCommand.channelName());
                    assertThat(channelSummary.description()).isEqualTo(createPublicCommand.channelDescription());
                    assertThat(channelSummary.lastMessageAt()).isNull();
                });
    }

    @Test
    @DisplayName("채널 상세 조회 성공 - 존재하지 않는 채널이면 Optional.empty 반환")
    void findByDetail_returnsEmpty_whenChannelDoesNotExist() {
        // given
        // 이 테스트는 findByDetail(...)에 전달한 channelId와 일치하는 Channel row가 없을 때
        // Optional.empty를 반환하는지 검증한다.
        //
        // 삭제 동작은 이 테스트의 관심사가 아니다.
        // 채널을 저장한 뒤 deleteById(...)로 지우면 findByDetail(...)뿐 아니라 삭제 쿼리와 flush 동작까지
        // 함께 검증하게 되므로, 여기서는 처음부터 존재하지 않는 UUID를 조회한다.
        //
        // 다만 테이블이 완전히 비어 있으면 "어떤 id로 조회해도 empty"라는 너무 약한 상황이 된다.
        // 그래서 다른 채널을 하나 저장해 둔 상태에서, 그 id와 다른 missingChannelId로 조회한다.
        PublicChannelFixture publicChannelFixture = savePublicChannel();
        Channel publicChannel = publicChannelFixture.channel();

        UUID existingChannelId = publicChannel.getId();
        UUID missingChannelId = UUID.randomUUID();

        // 사전 조건을 먼저 확인한다.
        // DB에는 실제 채널이 하나 존재해야 하고, 조회할 missingChannelId는 그 채널의 id와 달라야 한다.
        // 이 전제가 맞아야 findByDetail(...)의 where c.id = :channelId 조건이 제대로 적용되는지 검증할 수 있다.
        assertThat(existingChannelId).isNotNull();
        assertThat(missingChannelId).isNotNull();
        assertThat(missingChannelId).isNotEqualTo(existingChannelId);
        assertThat(channelRepository.existsById(existingChannelId)).isTrue();
        assertThat(channelRepository.existsById(missingChannelId)).isFalse();

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있던 Channel 객체가 조회 결과에 영향을 줄 수 없다.
        // 아래 when 절의 channelSummary는 실제 JPQL SELECT 결과다.
        em.clear();

        // when
        Optional<ChannelSummary> channelSummary = channelRepository.findByDetail(missingChannelId);

        // then
        // channelId가 일치하는 Channel row가 없으므로 projection을 만들 수 없고 Optional.empty가 반환되어야 한다.
        // DB에 다른 채널이 존재하더라도 다른 id의 채널이 잘못 반환되면 안 된다.
        assertThat(channelSummary).isEmpty();
    }

    @Test
    @DisplayName("요청한 채널만 일괄 조회하며 채널별 최신 메시지와 메시지 없는 채널을 포함한다")
    void findSummariesByIdIn_returnsRequestedChannelsWithLatestMessageAt() {
        User author = saveUser("batch-author");
        Channel withMessages = savePublicChannel().channel();
        Channel withoutMessages = savePrivateChannelFor(author).channel();
        Channel excluded = savePublicChannel().channel();
        Message old = saveMessage(author, withMessages, "old");
        Message latest = saveMessage(author, withMessages, "latest");
        saveMessage(author, excluded, "excluded");
        Instant oldTime = Instant.parse("2026-01-01T00:00:00Z");
        Instant latestTime = oldTime.plusSeconds(60);
        em.createQuery("update Message m set m.createdAt = :time where m.id = :id")
                .setParameter("time", oldTime).setParameter("id", old.getId()).executeUpdate();
        em.createQuery("update Message m set m.createdAt = :time where m.id = :id")
                .setParameter("time", latestTime).setParameter("id", latest.getId()).executeUpdate();
        em.clear();

        assertThat(channelRepository.findSummariesByIdIn(
                List.of(withMessages.getId(), withoutMessages.getId(), UUID.randomUUID())))
                .extracting(ChannelSummary::id, ChannelSummary::type, ChannelSummary::name,
                        ChannelSummary::description, ChannelSummary::lastMessageAt)
                .containsExactlyInAnyOrder(
                        tuple(withMessages.getId(), withMessages.getType(), withMessages.getName(),
                                withMessages.getDescription(), latestTime),
                        tuple(withoutMessages.getId(), withoutMessages.getType(), withoutMessages.getName(),
                                withoutMessages.getDescription(), null));
    }

    // ChannelRepository는 JPQL projection, exists subquery, left join 집계를 실제 DB에서 검증하는 테스트 대상이다.
    // 따라서 User를 mock으로 대체하지 않고 실제 row로 저장해 ReadStatus와 Message의 FK 조건이 JPA 매핑을 통과하게 한다.
    private User saveUser(String username) {
        UserCreateCommand command = new UserCreateCommand(
                username,
                username + "Password",
                username + "Email@gmail.com"
        );

        return userRepository.saveAndFlush(new User(command, null));
    }

    // PUBLIC 채널은 findVisibleChannelIds(...)에서 ReadStatus 없이도 항상 보이는 대상이다.
    // command와 entity를 함께 반환해 given 절은 짧게 유지하고, then 절에서는 projection의 name/description을 원본 command와 비교한다.
    private PublicChannelFixture savePublicChannel() {
        ChannelCreatePublicCommand command = new ChannelCreatePublicCommand(
                "publicChannel",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );

        return new PublicChannelFixture(channelRepository.saveAndFlush(new Channel(command)), command);
    }

    // PRIVATE 채널의 command.participantIds는 생성 입력값이고, visible 여부의 실제 기준은 ReadStatus row다.
    // 그래서 private channel 생성과 참여 row 생성을 분리해 테스트마다 "ReadStatus를 만들었는지"가 드러나게 한다.
    private PrivateChannelFixture savePrivateChannelFor(User participant) {
        ChannelCreatePrivateCommand command = new ChannelCreatePrivateCommand(
                List.of(participant.getId()),
                ChannelType.PRIVATE
        );

        return new PrivateChannelFixture(channelRepository.saveAndFlush(new Channel(command)), command);
    }

    // findVisibleChannelIds(...)의 PRIVATE 참여 조건은 read_statuses 테이블에 해당 user/channel 조합이 존재하는지다.
    // 참여가 필요한 테스트에서만 이 헬퍼를 호출해, 참여 여부 fixture를 명시적으로 만든다.
    private ReadStatus joinChannel(Channel channel, User user) {
        return readStatusRepository.saveAndFlush(
                new ReadStatus(
                        channel,
                        user,
                        new ReadStatusCreateCommand(user.getId(), Instant.now())
                )
        );
    }

    // lastMessageAt 검증은 Message.createdAt을 DB에서 max(...)로 집계하는지 확인한다.
    // 실제 Message row를 저장해야 left join과 집계가 Repository 쿼리 경로 그대로 검증된다.
    private Message saveMessage(User author, Channel channel, String content) {
        MessageCreateCommand command = new MessageCreateCommand(
                content,
                author.getId(),
                channel.getId()
        );

        return messageRepository.saveAndFlush(new Message(author, channel, command));
    }

    private record PublicChannelFixture(Channel channel, ChannelCreatePublicCommand command) {
    }

    private record PrivateChannelFixture(Channel channel, ChannelCreatePrivateCommand command) {
    }
}
