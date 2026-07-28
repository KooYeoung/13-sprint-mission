package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.config.JpaAuditingTestConfig;
import com.sprint.mission.discodeit.config.P6SpySqlFormatter;
import com.sprint.mission.discodeit.config.QuerydslTestConfig;
import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePublicCommand;
import com.sprint.mission.discodeit.dto.command.message.MessageCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false)
@Import(value = {QuerydslTestConfig.class, JpaAuditingTestConfig.class, P6SpySqlFormatter.class})
@DisplayName("MessageRepository JPA 테스트")
class MessageRepositoryTest {

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserStatusRepository userStatusRepository;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    BinaryContentRepository binaryContentRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("메시지 단건 조회 성공 - 작성자, 채널, 작성자 상태, 프로필을 함께 조회")
    void findById_fetchesAuthorChannelAuthorStatusAndProfile_whenMessageExists() {
        // given
        // 이 테스트의 대상은 MessageRepository.findById(...)다.
        // MessageRepository.findById(...)에는 EntityGraph가 선언되어 있고,
        // Message.author, Message.channel, author.userStatus, author.profile을 함께 조회해야 한다.
        //
        // 단순히 Message row만 저장하고 content만 확인하면 findById의 fetch 계약을 검증할 수 없다.
        // 그래서 작성자 프로필(BinaryContent), 작성자 상태(UserStatus), 채널까지 모두 실제 엔티티로 저장한 뒤
        // 영속성 컨텍스트를 비우고 DB에서 다시 조회해 EntityGraph 적용 여부를 확인한다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        // 조회 대상 메시지다.
        // findById(savedMessageId)는 이 메시지와 연결된 author/channel을 함께 반환해야 한다.
        MessageCreateCommand messageCreateCommand = new MessageCreateCommand(
                "testMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, messageCreateCommand)
        );

        // 대조군 메시지를 추가해 findById(...)가 요청한 id의 메시지만 반환하는지 확인한다.
        // 같은 작성자와 채널을 공유하더라도 message id가 다르면 조회 결과가 섞이면 안 된다.
        MessageCreateCommand otherMessageCreateCommand = new MessageCreateCommand(
                "otherMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedOtherMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, otherMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedMessageId = savedMessage.getId();
        UUID savedOtherMessageId = savedOtherMessage.getId();
        UUID savedChannelId = savedChannel.getId();

        // 테스트 fixture가 의도대로 저장됐는지 확인한다.
        // 여기서 id가 null이면 findById 검증 실패 원인을 Repository 문제가 아니라 given 구성 문제로 봐야 한다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedMessageId).isNotNull();
        assertThat(savedOtherMessageId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherMessageId).isNotEqualTo(savedMessageId);
        assertThat(savedMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherMessage.getChannelId()).isEqualTo(savedChannelId);

        // 저장 직후 영속성 컨텍스트를 비운다.
        // 그래야 아래 findById(...)가 1차 캐시에 있는 엔티티를 반환하는 것이 아니라,
        // 실제 DB 조회와 EntityGraph 적용 결과를 보여준다.
        em.clear();

        // when
        // Message의 id로 단건 조회한다.
        // 기존 코드처럼 channelId를 넘기면 Message PK 조회가 아니므로 테스트 대상 자체가 달라진다.
        Message foundMessage = messageRepository.findById(savedMessageId).orElseThrow(AssertionError::new);

        // then
        // 조회된 Message의 식별자와 값, 그리고 EntityGraph로 함께 로딩되어야 하는 연관 엔티티들을 검증한다.
        assertFetchedMessageFixture(
                foundMessage,
                savedMessageId,
                messageCreateCommand,
                savedAuthorId,
                authorCreateCommand,
                savedAuthorStatusId,
                savedProfile,
                channelCreateCommand,
                savedChannelId
        );
    }

    @Test
    @DisplayName("메시지 단건 조회 성공 - 존재하지 않는 메시지이면 Optional.empty 반환")
    void findById_returnsEmpty_whenMessageDoesNotExist() {
        // given
        // 이 테스트의 대상은 MessageRepository.findById(...)가 존재하지 않는 Message PK를 받았을 때
        // Optional.empty를 반환하는지다.
        //
        // 빈 DB에서 임의 UUID를 조회하면 당연히 empty가 나오므로, 그 경우만으로는
        // "id 조건이 정확히 적용됐다"는 근거가 약하다. 그래서 조회 대상이 아닌 Message row를 실제로 저장해
        // messages 테이블에는 데이터가 있지만 요청한 id의 row만 없는 상황을 만든다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        // Message.author는 nullable이지만 실제 서비스 흐름의 메시지는 작성자를 가진다.
        // findById(...)에는 author, author.userStatus, author.profile EntityGraph도 걸려 있으므로,
        // 성공 조회가 가능한 완전한 메시지 fixture를 만들어 두면 음성 케이스의 전제도 더 선명해진다.
        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        // 작성자 상태는 음성 조회 결과 자체에는 직접 사용되지 않는다.
        // 다만 저장된 메시지를 사전 조회할 때 findById(...)의 EntityGraph가 접근할 수 있는 연관 row로 둔다.
        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        // Message.channel은 nullable = false 연관관계다.
        // 따라서 메시지를 실제 DB에 저장하려면 먼저 실제 Channel row가 필요하다.
        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        // 첫 번째 메시지는 "존재하는 id로는 findById(...)가 정상 조회된다"는 사전 검증 대상이다.
        MessageCreateCommand messageCreateCommand = new MessageCreateCommand(
                "testMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, messageCreateCommand)
        );

        // 두 번째 메시지는 대조군이다.
        // messages 테이블에 row가 하나만 있는 상황보다, 여러 row 중에서도 요청한 id가 없을 때 empty가 나오는지를
        // 확인하는 편이 "PK 조건으로 정확히 조회한다"는 의도를 더 잘 드러낸다.
        MessageCreateCommand otherMessageCreateCommand = new MessageCreateCommand(
                "otherMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedOtherMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, otherMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedMessageId = savedMessage.getId();
        UUID savedOtherMessageId = savedOtherMessage.getId();
        UUID savedChannelId = savedChannel.getId();

        UUID nonExistentMessageId;
        do {
            nonExistentMessageId = UUID.randomUUID();
        } while (nonExistentMessageId.equals(savedMessageId) || nonExistentMessageId.equals(savedOtherMessageId));

        // 사전 조건을 먼저 고정한다.
        // id가 null이거나 대조군 id가 서로 같으면 이후 Optional.empty 검증이 Repository 동작 실패인지,
        // given fixture 구성 실패인지 구분하기 어렵다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedMessageId).isNotNull();
        assertThat(savedOtherMessageId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherMessageId).isNotEqualTo(savedMessageId);
        assertThat(nonExistentMessageId)
                .isNotEqualTo(savedMessageId)
                .isNotEqualTo(savedOtherMessageId);
        assertThat(savedMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(messageRepository.count()).isEqualTo(2);

        // 저장 직후의 Message, User, Channel 등이 1차 캐시에 남아 있어도 missing id 조회에는 직접 영향이 없지만,
        // Repository 테스트에서는 실제 DB 조회 경로를 명확히 하기 위해 영속성 컨텍스트를 비운다.
        em.clear();

        // 조회 대상이 아닌 메시지는 실제로 DB에서 찾을 수 있어야 한다.
        // 이 사전 검증이 있어야 아래 empty 결과가 "테이블이 비어서"가 아니라
        // "조회한 Message PK와 일치하는 row가 없어서" 발생했다는 점이 분명해진다.
        assertMessageDetails(savedMessageId, messageCreateCommand, savedChannelId);
        assertMessageDetails(savedOtherMessageId, otherMessageCreateCommand, savedChannelId);

        // when
        // messages 테이블에 존재하지 않는 Message PK로 단건 조회한다.
        Optional<Message> optionalMessage = messageRepository.findById(nonExistentMessageId);

        // then
        // 같은 작성자와 채널에 연결된 다른 메시지 row가 존재하더라도,
        // 요청한 id와 일치하는 메시지가 없으면 Optional.empty가 반환되어야 한다.
        assertThat(optionalMessage).isEmpty();
    }


    @Test
    @DisplayName("채널 최신 메시지 조회 성공 - 채널의 가장 최근 메시지 반환")
    void findTop1ByChannel_IdOrderByCreatedAtDesc_returnsLatestMessage_whenChannelHasMessages() throws InterruptedException {
        // given
        // 이 테스트의 대상은 MessageRepository.findTop1ByChannel_IdOrderByCreatedAtDesc(...)다.
        // 메서드 이름에서 드러나는 계약은 세 가지다.
        // 1. channel.id 조건으로 특정 채널의 메시지만 조회한다.
        // 2. createdAt 내림차순으로 정렬한다.
        // 3. 그중 첫 번째 메시지만 Optional에 담아 반환한다.
        //
        // 따라서 단순히 메시지 1건만 저장하면 정렬과 채널 조건을 검증할 수 없다.
        // 대상 채널에는 메시지 2건을 저장하고, 다른 채널에는 더 늦게 생성된 메시지를 저장해
        // "전체 메시지 중 최신"이 아니라 "요청한 채널 안에서 최신" 메시지를 반환하는지 확인한다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고 실제 Entity를 저장한다.
        // Message.author는 nullable이지만 실제 서비스 흐름의 메시지는 작성자를 가지므로 실제 User를 연결한다.
        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        // findTop1ByChannel_IdOrderByCreatedAtDesc(...)는 author 상태를 조회하는 메서드는 아니다.
        // 다만 메시지 fixture를 실제 서비스 데이터에 가깝게 구성하기 위해 작성자 상태도 실제 row로 저장한다.
        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        // Message.channel은 nullable = false 연관관계다.
        // targetChannel은 조회 대상 채널이고, otherChannel은 channel.id 조건 검증을 위한 대조군 채널이다.
        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        ChannelCreatePublicCommand otherChannelCreateCommand = new ChannelCreatePublicCommand(
                "otherPublicChannelName",
                "otherPublicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedOtherChannel = channelRepository.saveAndFlush(new Channel(otherChannelCreateCommand));

        // 대상 채널의 오래된 메시지다.
        // 아래 최신 메시지와 createdAt이 명확히 달라야 order by createdAt desc 검증이 의미를 가진다.
        MessageCreateCommand messageCreateCommand = new MessageCreateCommand(
                "testMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, messageCreateCommand)
        );

        // createdAt은 JPA Auditing이 저장 시점에 채운다.
        // H2 timestamp 정밀도와 실행 속도 때문에 두 메시지가 같은 createdAt을 갖지 않도록 짧게 대기한다.
        Thread.sleep(100);

        // 대상 채널의 최신 메시지다.
        // 이 메시지가 findTop1ByChannel_IdOrderByCreatedAtDesc(savedChannelId)의 반환 대상이어야 한다.
        MessageCreateCommand otherMessageCreateCommand = new MessageCreateCommand(
                "otherMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedOtherMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, otherMessageCreateCommand)
        );

        Thread.sleep(100);

        // 다른 채널의 메시지는 대상 채널의 최신 메시지보다 더 늦게 저장한다.
        // 이 row가 있어야 Repository 메서드가 channel.id 조건 없이 전체 최신 메시지를 고르는 버그를 잡을 수 있다.
        MessageCreateCommand otherChannelMessageCreateCommand = new MessageCreateCommand(
                "otherChannelMessageContent",
                savedAuthor.getId(),
                savedOtherChannel.getId()
        );
        Message savedOtherChannelMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedOtherChannel, otherChannelMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedMessageId = savedMessage.getId();
        UUID savedOtherMessageId = savedOtherMessage.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();
        UUID savedOtherChannelMessageId = savedOtherChannelMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 여기서 id나 createdAt이 비정상이면 최신 메시지 조회 실패인지 fixture 구성 실패인지 구분하기 어렵다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedMessageId).isNotNull();
        assertThat(savedOtherMessageId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedOtherChannelMessageId).isNotNull();
        assertThat(savedOtherMessageId).isNotEqualTo(savedMessageId);
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessageId)
                .isNotEqualTo(savedMessageId)
                .isNotEqualTo(savedOtherMessageId);
        assertThat(savedMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
        assertThat(savedMessage.getCreatedAt()).isNotNull();
        assertThat(savedOtherMessage.getCreatedAt()).isNotNull();
        assertThat(savedOtherChannelMessage.getCreatedAt()).isNotNull();
        assertThat(savedOtherMessage.getCreatedAt()).isAfter(savedMessage.getCreatedAt());
        assertThat(savedOtherChannelMessage.getCreatedAt()).isAfter(savedOtherMessage.getCreatedAt());
        assertThat(messageRepository.count()).isEqualTo(3);

        // 저장 직후의 Message가 1차 캐시에 남아 있으면 정렬 쿼리 결과와 캐시 상태를 혼동할 수 있다.
        // 영속성 컨텍스트를 비워 아래 when 절이 실제 DB 조회와 order by 결과를 기준으로 검증되게 한다.
        em.clear();

        // when
        // 대상 채널 id로 최신 메시지 1건을 조회한다.
        Message message = messageRepository.findTop1ByChannel_IdOrderByCreatedAtDesc(savedChannelId)
                .orElseThrow(AssertionError::new);

        // then
        // 대상 채널에는 메시지가 2건 있고, 그중 createdAt이 더 늦은 savedOtherMessage가 반환되어야 한다.
        // 다른 채널의 메시지가 전체 DB에서 더 최신이어도 channel.id 조건 때문에 반환되면 안 된다.

        assertThat(message.getId()).isEqualTo(savedOtherMessageId);
        assertThat(message.getContent()).isEqualTo(otherMessageCreateCommand.content());
        assertThat(message.getChannelId()).isEqualTo(savedChannelId);

        // DB에서 다시 조회한 createdAt은 timestamp 정밀도 때문에 저장 직후 엔티티의 Instant와
        // 나노초 단위까지 완전히 같지 않을 수 있다. 그래서 H2의 마이크로초 정밀도 수준의 오차만 허용한다.
        assertThat(Duration.between(message.getCreatedAt(), savedOtherMessage.getCreatedAt()).abs())
                .isLessThanOrEqualTo(Duration.ofNanos(1_000));
    }

    @Test
    @DisplayName("채널 최신 메시지 조회 성공 - 채널에 메시지가 없으면 Optional.empty 반환")
    void findTop1ByChannel_IdOrderByCreatedAtDesc_returnsEmpty_whenChannelHasNoMessages() {
        // given
        // 이 테스트의 대상은 MessageRepository.findTop1ByChannel_IdOrderByCreatedAtDesc(...)가
        // "존재하지만 메시지가 없는 채널"에 대해 Optional.empty를 반환하는지다.
        //
        // 존재하지 않는 임의 channelId를 조회하면 empty가 나오더라도,
        // 그것은 "채널이 없어서"인지 "채널은 있지만 메시지가 없어서"인지 구분하기 어렵다.
        // 따라서 조회 대상 Channel row는 실제로 저장하되, 그 채널에는 Message를 저장하지 않는다.
        //
        // 동시에 다른 채널에는 Message를 저장해 둔다.
        // 이 대조군이 있어야 messages 테이블 전체가 비어서 우연히 empty가 되는 테스트가 아니라,
        // channel.id 조건이 적용되어 대상 채널의 메시지만 찾는다는 점을 확인할 수 있다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고 실제 Entity를 저장한다.
        // 대조군 메시지를 만들려면 작성자가 필요하므로 실제 User를 저장한다.
        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        // 작성자 상태는 findTop1ByChannel_IdOrderByCreatedAtDesc(...)의 직접 검증 대상은 아니다.
        // 다만 대조군 메시지 fixture를 실제 서비스 데이터에 가깝게 만들기 위해 실제 row로 저장한다.
        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        // Message.channel은 nullable = false 연관관계다.
        // savedChannel은 조회 대상 채널이다. 이 채널에는 메시지를 하나도 연결하지 않는다.
        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        // savedOtherChannel은 대조군 채널이다.
        // messages 테이블에 row가 존재하는 상황을 만들기 위해 이 채널에만 메시지를 연결한다.
        ChannelCreatePublicCommand otherChannelCreateCommand = new ChannelCreatePublicCommand(
                "otherPublicChannelName",
                "otherPublicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedOtherChannel = channelRepository.saveAndFlush(new Channel(otherChannelCreateCommand));

        // 대조군 메시지다.
        // 이 메시지가 존재하더라도 savedChannel에는 메시지가 없으므로 savedChannelId 조회 결과는 empty여야 한다.
        MessageCreateCommand otherChannelMessageCreateCommand = new MessageCreateCommand(
                "otherChannelMessageContent",
                savedAuthor.getId(),
                savedOtherChannel.getId()
        );
        Message savedOtherChannelMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedOtherChannel, otherChannelMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();
        UUID savedOtherChannelMessageId = savedOtherChannelMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 여기서 id가 null이거나 채널이 구분되지 않으면 이후 empty 검증이 Repository 동작 실패인지,
        // given fixture 구성 실패인지 구분하기 어렵다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedOtherChannelMessageId).isNotNull();
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
        assertThat(savedOtherChannelMessage.getCreatedAt()).isNotNull();
        assertThat(messageRepository.count()).isEqualTo(1);

        // 저장 직후의 Channel과 Message가 1차 캐시에 남아 있어도 empty 조회에는 직접 영향이 크지 않지만,
        // Repository 테스트에서는 실제 DB 조회 경로를 명확히 하기 위해 영속성 컨텍스트를 비운다.
        em.clear();

        // 대조군 채널에는 메시지가 실제로 존재해야 한다.
        // 이 사전 조회가 있어야 아래 empty 결과가 "messages 테이블이 비어서"가 아니라
        // "조회 대상 채널에 연결된 메시지만 없어서" 발생했다는 점이 분명해진다.
        assertThat(messageRepository.findTop1ByChannel_IdOrderByCreatedAtDesc(savedOtherChannelId))
                .hasValueSatisfying(foundOtherChannelMessage -> {
                    assertThat(foundOtherChannelMessage.getId()).isEqualTo(savedOtherChannelMessageId);
                    assertThat(foundOtherChannelMessage.getContent()).isEqualTo(otherChannelMessageCreateCommand.content());
                    assertThat(foundOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
                });

        // when
        // DB에 실제로 존재하지만 메시지가 하나도 연결되지 않은 채널 id로 최신 메시지를 조회한다.
        Optional<Message> top1ByChannelIdOrderByCreatedAtDesc = messageRepository.findTop1ByChannel_IdOrderByCreatedAtDesc(savedChannelId);

        // then
        // 다른 채널에는 메시지가 존재하더라도, 조회 대상 채널에 연결된 메시지가 없으면 Optional.empty가 반환되어야 한다.
        assertThat(top1ByChannelIdOrderByCreatedAtDesc).isEmpty();
    }

    @Test
    @DisplayName("채널별 메시지 Slice 조회 성공 - cursor가 없으면 최신순으로 첫 페이지 반환")
    void findAllByChannelId_returnsMessagesOrderedByCreatedAtDesc_whenCursorIsNull() throws InterruptedException {
        // given
        // 이 테스트의 대상은 MessageRepositoryCustomImpl.findAllByChannelId(...)다.
        // cursor가 null이면 첫 페이지 조회이므로 createdAt 조건 없이 대상 채널의 메시지를 최신순으로 가져와야 한다.
        //
        // 검증해야 하는 계약은 세 가지다.
        // 1. channelId와 일치하는 메시지만 조회한다.
        // 2. cursor가 null이면 최신 메시지부터 createdAt desc 순서로 조회한다.
        // 3. pageSize만큼만 Slice content에 담고, 초과 데이터가 있으면 hasNext를 true로 반환한다.
        //
        // 그래서 대상 채널에는 메시지 3건을 오래된 순서로 저장하고, pageSize는 2로 둔다.
        // 다른 채널에는 대상 채널의 최신 메시지보다 더 늦게 생성된 메시지를 저장해
        // 전체 최신 메시지가 아니라 요청한 채널의 메시지만 첫 페이지에 포함되는지 확인한다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고 실제 Entity를 저장한다.
        // Message.author는 nullable이지만 실제 서비스 흐름의 메시지는 작성자를 가지므로 실제 User를 연결한다.
        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        // findAllByChannelId(...)는 메시지 목록을 반환하는 메서드다.
        // 커스텀 Querydsl 구현에서 author, profile, userStatus를 fetch join하므로 실제 연관 row를 함께 둔다.
        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        // Message.channel은 nullable = false 연관관계다.
        // savedChannel은 조회 대상 채널이고, savedOtherChannel은 channel.id 조건 검증을 위한 대조군 채널이다.
        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        ChannelCreatePublicCommand otherChannelCreateCommand = new ChannelCreatePublicCommand(
                "otherPublicChannelName",
                "otherPublicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedOtherChannel = channelRepository.saveAndFlush(new Channel(otherChannelCreateCommand));

        // 대상 채널의 가장 오래된 메시지다.
        // createdAt은 JPA Auditing이 저장 시점에 채우므로, 각 메시지 사이에 짧게 대기해 정렬 기준을 분명히 한다.
        MessageCreateCommand oldestMessageCreateCommand = new MessageCreateCommand(
                "oldestMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedOldestMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, oldestMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대상 채널의 중간 메시지다.
        // pageSize를 2로 조회하면 최신 메시지 다음 두 번째 항목으로 반환되어야 한다.
        MessageCreateCommand middleMessageCreateCommand = new MessageCreateCommand(
                "middleMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedMiddleMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, middleMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대상 채널의 최신 메시지다.
        // cursor가 null인 첫 페이지에서는 이 메시지가 content의 첫 번째 요소로 와야 한다.
        MessageCreateCommand newestMessageCreateCommand = new MessageCreateCommand(
                "newestMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedNewestMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, newestMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대조군 채널의 메시지다.
        // 대상 채널의 최신 메시지보다 더 늦게 저장되지만, channelId가 다르므로 조회 결과에 포함되면 안 된다.
        MessageCreateCommand otherChannelMessageCreateCommand = new MessageCreateCommand(
                "otherChannelMessageContent",
                savedAuthor.getId(),
                savedOtherChannel.getId()
        );
        Message savedOtherChannelMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedOtherChannel, otherChannelMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedOldestMessageId = savedOldestMessage.getId();
        UUID savedMiddleMessageId = savedMiddleMessage.getId();
        UUID savedNewestMessageId = savedNewestMessage.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();
        UUID savedOtherChannelMessageId = savedOtherChannelMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 여기서 id나 createdAt이 비정상이면 Slice 조회 실패인지 fixture 구성 실패인지 구분하기 어렵다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedOldestMessageId).isNotNull();
        assertThat(savedMiddleMessageId).isNotNull();
        assertThat(savedNewestMessageId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedOtherChannelMessageId).isNotNull();
        assertThat(savedMiddleMessageId).isNotEqualTo(savedOldestMessageId);
        assertThat(savedNewestMessageId)
                .isNotEqualTo(savedOldestMessageId)
                .isNotEqualTo(savedMiddleMessageId);
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessageId)
                .isNotEqualTo(savedOldestMessageId)
                .isNotEqualTo(savedMiddleMessageId)
                .isNotEqualTo(savedNewestMessageId);
        assertThat(savedOldestMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedMiddleMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedNewestMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
        assertThat(savedOldestMessage.getCreatedAt()).isNotNull();
        assertThat(savedMiddleMessage.getCreatedAt()).isNotNull();
        assertThat(savedNewestMessage.getCreatedAt()).isNotNull();
        assertThat(savedOtherChannelMessage.getCreatedAt()).isNotNull();
        assertThat(savedMiddleMessage.getCreatedAt()).isAfter(savedOldestMessage.getCreatedAt());
        assertThat(savedNewestMessage.getCreatedAt()).isAfter(savedMiddleMessage.getCreatedAt());
        assertThat(savedOtherChannelMessage.getCreatedAt()).isAfter(savedNewestMessage.getCreatedAt());
        assertThat(messageRepository.count()).isEqualTo(4);

        // 저장 직후의 Message가 1차 캐시에 남아 있으면 정렬 쿼리 결과와 캐시 상태를 혼동할 수 있다.
        // 영속성 컨텍스트를 비워 아래 when 절이 실제 Querydsl fetch join, where, order by, limit 결과를 기준으로 검증되게 한다.
        em.clear();

        // when
        // cursor를 null로 넘겨 첫 페이지를 조회한다.
        // PageRequest의 sort는 일부러 지정하지 않는다. 현재 커스텀 Repository 구현이 직접 createdAt desc를 적용하기 때문이다.
        Instant cursor = null;
        Pageable pageable = PageRequest.of(0, 2);
        Slice<Message> allByChannelId = messageRepository.findAllByChannelId(savedChannelId, pageable, cursor);

        // then
        // 첫 페이지 메타데이터가 요청한 Pageable을 반영해야 한다.
        assertThat(allByChannelId.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(allByChannelId.getPageable().getPageSize()).isEqualTo(2);

        // 대상 채널에는 3건이 있고 pageSize는 2이므로 content는 2건만 반환되어야 한다.
        // 구현은 pageSize + 1건을 조회해 다음 페이지 존재 여부를 판단한 뒤 초과분을 제거한다.
        assertThat(allByChannelId.getContent()).hasSize(2);
        assertThat(allByChannelId.hasNext()).isTrue();

        // cursor가 null인 첫 페이지는 createdAt desc 기준으로 최신 메시지부터 반환되어야 한다.
        // 대조군 채널 메시지가 전체 DB에서 가장 최신이어도 channelId가 다르므로 결과에 포함되면 안 된다.
        assertThat(allByChannelId.getContent())
                .extracting(Message::getId)
                .containsExactly(savedNewestMessageId, savedMiddleMessageId);
        assertThat(allByChannelId.getContent())
                .extracting(Message::getContent)
                .containsExactly(newestMessageCreateCommand.content(), middleMessageCreateCommand.content());
        assertThat(allByChannelId.getContent())
                .allSatisfy(message -> assertThat(message.getChannelId()).isEqualTo(savedChannelId));
    }

    @Test
    @DisplayName("채널별 메시지 Slice 조회 성공 - cursor보다 오래된 메시지만 반환")
    void findAllByChannelId_appliesCursorExclusive_whenCursorExists() throws InterruptedException {
        // given
        // 이 테스트의 대상은 MessageRepositoryCustomImpl.findAllByChannelId(...)의 cursor 조건이다.
        // cursor가 있으면 where 절에 message.createdAt.lt(cursor)가 적용되어,
        // cursor 시각과 같은 메시지는 제외하고 cursor보다 오래된 메시지만 반환해야 한다.
        //
        // 검증해야 하는 계약은 세 가지다.
        // 1. cursor보다 최신인 메시지는 제외한다.
        // 2. cursor와 createdAt이 같은 메시지도 제외한다. 즉, cursor는 exclusive 경계다.
        // 3. cursor보다 오래된 메시지라도 channelId가 다르면 제외한다.
        //
        // 그래서 대상 채널에는 오래된 메시지, cursor로 사용할 중간 메시지, 최신 메시지를 저장한다.
        // 대조군 채널에는 cursor보다 오래된 메시지를 저장해,
        // channelId 조건이 빠지면 결과에 섞일 수 있는 상황을 만든다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고 실제 Entity를 저장한다.
        // Message.author는 nullable이지만 실제 서비스 흐름의 메시지는 작성자를 가지므로 실제 User를 연결한다.
        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        // findAllByChannelId(...)는 메시지 목록을 반환하는 메서드다.
        // 커스텀 Querydsl 구현에서 author, profile, userStatus를 fetch join하므로 실제 연관 row를 함께 둔다.
        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        // Message.channel은 nullable = false 연관관계다.
        // savedChannel은 조회 대상 채널이고, savedOtherChannel은 channel.id 조건 검증을 위한 대조군 채널이다.
        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        ChannelCreatePublicCommand otherChannelCreateCommand = new ChannelCreatePublicCommand(
                "otherPublicChannelName",
                "otherPublicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedOtherChannel = channelRepository.saveAndFlush(new Channel(otherChannelCreateCommand));

        // 대조군 채널의 메시지다.
        // 이 메시지는 cursor보다 오래되도록 가장 먼저 저장한다.
        // channelId 조건이 빠진 구현이라면 이 메시지가 결과에 섞일 수 있으므로 좋은 대조군이 된다.
        MessageCreateCommand otherChannelMessageCreateCommand = new MessageCreateCommand(
                "otherChannelMessageContent",
                savedAuthor.getId(),
                savedOtherChannel.getId()
        );
        Message savedOtherChannelMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedOtherChannel, otherChannelMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대상 채널의 가장 오래된 메시지다.
        // cursor보다 오래된 대상 채널 메시지이므로 조회 결과에 포함되어야 한다.
        MessageCreateCommand oldestMessageCreateCommand = new MessageCreateCommand(
                "oldestMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedOldestMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, oldestMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대상 채널의 중간 메시지다. 이 메시지의 createdAt을 cursor로 사용한다.
        // 구현이 lt(cursor)가 아니라 lte(cursor)를 사용하면 이 메시지가 결과에 포함되어 테스트가 실패해야 한다.
        MessageCreateCommand middleMessageCreateCommand = new MessageCreateCommand(
                "middleMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedMiddleMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, middleMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대상 채널의 최신 메시지다.
        // cursor보다 최신인 메시지이므로 조회 결과에 포함되면 안 된다.
        MessageCreateCommand newestMessageCreateCommand = new MessageCreateCommand(
                "newestMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedNewestMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, newestMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedOtherChannelMessageId = savedOtherChannelMessage.getId();
        UUID savedOldestMessageId = savedOldestMessage.getId();
        UUID savedMiddleMessageId = savedMiddleMessage.getId();
        UUID savedNewestMessageId = savedNewestMessage.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();

        // 사전 조건을 먼저 고정한다.
        // 여기서 id나 createdAt 순서가 비정상이면 cursor 조건 검증 실패인지 fixture 구성 실패인지 구분하기 어렵다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedOtherChannelMessageId).isNotNull();
        assertThat(savedOldestMessageId).isNotNull();
        assertThat(savedMiddleMessageId).isNotNull();
        assertThat(savedNewestMessageId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedMiddleMessageId).isNotEqualTo(savedOldestMessageId);
        assertThat(savedNewestMessageId)
                .isNotEqualTo(savedOldestMessageId)
                .isNotEqualTo(savedMiddleMessageId);
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessageId)
                .isNotEqualTo(savedOldestMessageId)
                .isNotEqualTo(savedMiddleMessageId)
                .isNotEqualTo(savedNewestMessageId);
        assertThat(savedOldestMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedMiddleMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedNewestMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
        assertThat(savedOtherChannelMessage.getCreatedAt()).isNotNull();
        assertThat(savedOldestMessage.getCreatedAt()).isNotNull();
        assertThat(savedMiddleMessage.getCreatedAt()).isNotNull();
        assertThat(savedNewestMessage.getCreatedAt()).isNotNull();
        assertThat(savedOtherChannelMessage.getCreatedAt()).isBefore(savedOldestMessage.getCreatedAt());
        assertThat(savedMiddleMessage.getCreatedAt()).isAfter(savedOldestMessage.getCreatedAt());
        assertThat(savedNewestMessage.getCreatedAt()).isAfter(savedMiddleMessage.getCreatedAt());
        assertThat(messageRepository.count()).isEqualTo(4);

        // 저장 직후의 Message가 1차 캐시에 남아 있으면 정렬 쿼리 결과와 캐시 상태를 혼동할 수 있다.
        // 먼저 영속성 컨텍스트를 비워 cursor도 DB에 저장된 timestamp 값을 기준으로 다시 읽는다.
        //
        // savedMiddleMessage.getCreatedAt()은 JPA Auditing이 메모리에 넣은 값이다.
        // DB timestamp 정밀도 때문에 저장/조회 과정에서 나노초 값이 달라질 수 있으므로,
        // 실제 목록 API가 이전 페이지 응답에서 받은 cursor처럼 DB에서 재조회한 createdAt을 사용한다.
        em.clear();
        Instant cursor = messageRepository.findById(savedMiddleMessageId)
                .orElseThrow(AssertionError::new)
                .getCreatedAt();
        assertThat(cursor).isNotNull();

        // cursor를 읽기 위해 findById(...)가 중간 메시지를 영속성 컨텍스트에 올렸으므로 한 번 더 비운다.
        // 그래야 아래 when 절의 Slice 조회가 1차 캐시가 아니라 실제 Querydsl fetch join, where, order by, limit 결과를 기준으로 검증된다.
        em.clear();

        // when
        // 중간 메시지의 createdAt을 cursor로 넘긴다.
        // Querydsl 조건은 message.createdAt.lt(cursor)이므로, 중간 메시지 자신과 그 이후 메시지는 제외되어야 한다.
        // PageRequest의 sort는 일부러 지정하지 않는다. 현재 커스텀 Repository 구현이 직접 createdAt desc를 적용하기 때문이다.
        Pageable pageable = PageRequest.of(0, 2);
        Slice<Message> allByChannelId = messageRepository.findAllByChannelId(savedChannelId, pageable, cursor);

        // then
        // Slice 메타데이터가 요청한 Pageable을 반영해야 한다.
        assertThat(allByChannelId.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(allByChannelId.getPageable().getPageSize()).isEqualTo(2);

        // 대상 채널에서 cursor보다 오래된 메시지는 savedOldestMessage 1건뿐이다.
        // pageSize가 2여도 조회 대상이 1건뿐이므로 hasNext는 false여야 한다.
        assertThat(allByChannelId.getContent()).hasSize(1);
        assertThat(allByChannelId.hasNext()).isFalse();

        // savedMiddleMessage는 cursor와 createdAt이 같으므로 exclusive 조건에 의해 제외되어야 한다.
        // savedNewestMessage는 cursor보다 최신이라 제외되어야 한다.
        // savedOtherChannelMessage는 cursor보다 오래되지만 channelId가 다르므로 제외되어야 한다.
        // 결과적으로 대상 채널의 오래된 메시지 1건만 남아야 한다.
        assertThat(allByChannelId.getContent())
                .extracting(Message::getId)
                .containsExactly(savedOldestMessageId);
        assertThat(allByChannelId.getContent())
                .extracting(Message::getContent)
                .containsExactly(oldestMessageCreateCommand.content());
        assertThat(allByChannelId.getContent())
                .allSatisfy(message -> assertThat(message.getChannelId()).isEqualTo(savedChannelId));
    }

    @Test
    @DisplayName("채널별 메시지 Slice 조회 성공 - pageSize보다 결과가 많으면 hasNext true 반환")
    void findAllByChannelId_returnsSliceWithHasNextTrue_whenResultSizeExceedsPageSize() throws InterruptedException {
        // given
        // 이 테스트의 대상은 MessageRepositoryCustomImpl.findAllByChannelId(...)의 Slice hasNext 계산이다.
        // 현재 구현은 pageSize보다 1건 더 조회한다.
        //
        // 1. limit(pageSize + 1)로 실제 조회 대상이 다음 페이지를 가질 만큼 충분한지 확인한다.
        // 2. 조회 결과가 pageSize보다 많으면 hasNext를 true로 둔다.
        // 3. 클라이언트에 반환할 content에서는 초과로 조회한 1건을 제거한다.
        //
        // 따라서 대상 채널에 메시지 3건을 저장하고 pageSize를 2로 조회한다.
        // 이 경우 Repository 내부에서는 3건을 읽어 hasNext=true를 계산하되,
        // Slice content에는 pageSize에 맞춰 최신 2건만 남겨야 한다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고 실제 Entity를 저장한다.
        // Message.author는 nullable이지만 실제 서비스 흐름의 메시지는 작성자를 가지므로 실제 User를 연결한다.
        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        // findAllByChannelId(...)는 메시지 목록을 반환하는 메서드다.
        // 커스텀 Querydsl 구현에서 author, profile, userStatus를 fetch join하므로 실제 연관 row를 함께 둔다.
        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        // Message.channel은 nullable = false 연관관계다.
        // savedChannel은 조회 대상 채널이고, savedOtherChannel은 channel.id 조건 검증을 위한 대조군 채널이다.
        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        // 대상 채널의 가장 오래된 메시지다.
        // 이 메시지는 pageSize + 1로 조회되는 초과분이며, hasNext 계산에는 사용되지만 content에서는 제거되어야 한다.
        MessageCreateCommand oldestMessageCreateCommand = new MessageCreateCommand(
                "oldestMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedOldestMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, oldestMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대상 채널의 중간 메시지다.
        // pageSize가 2일 때 content의 두 번째 요소로 남아야 한다.
        MessageCreateCommand middleMessageCreateCommand = new MessageCreateCommand(
                "middleMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedMiddleMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, middleMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대상 채널의 최신 메시지다.
        // cursor가 null인 첫 페이지에서 content의 첫 번째 요소로 반환되어야 한다.
        MessageCreateCommand newestMessageCreateCommand = new MessageCreateCommand(
                "newestMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedNewestMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, newestMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedOldestMessageId = savedOldestMessage.getId();
        UUID savedMiddleMessageId = savedMiddleMessage.getId();
        UUID savedNewestMessageId = savedNewestMessage.getId();
        UUID savedChannelId = savedChannel.getId();

        // 사전 조건을 먼저 고정한다.
        // 여기서 id나 createdAt 순서가 비정상이면 hasNext 계산 실패인지 fixture 구성 실패인지 구분하기 어렵다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedOldestMessageId).isNotNull();
        assertThat(savedMiddleMessageId).isNotNull();
        assertThat(savedNewestMessageId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedMiddleMessageId).isNotEqualTo(savedOldestMessageId);
        assertThat(savedNewestMessageId)
                .isNotEqualTo(savedOldestMessageId)
                .isNotEqualTo(savedMiddleMessageId);
        assertThat(savedOldestMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedMiddleMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedNewestMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOldestMessage.getCreatedAt()).isNotNull();
        assertThat(savedMiddleMessage.getCreatedAt()).isNotNull();
        assertThat(savedNewestMessage.getCreatedAt()).isNotNull();
        assertThat(savedMiddleMessage.getCreatedAt()).isAfter(savedOldestMessage.getCreatedAt());
        assertThat(savedNewestMessage.getCreatedAt()).isAfter(savedMiddleMessage.getCreatedAt());
        assertThat(messageRepository.count()).isEqualTo(3);

        // 저장 직후의 Message가 1차 캐시에 남아 있으면 정렬 쿼리 결과와 캐시 상태를 혼동할 수 있다.
        // 영속성 컨텍스트를 비워 아래 when 절이 실제 Querydsl fetch join, where, order by, limit 결과를 기준으로 검증되게 한다.
        em.clear();

        // when
        // cursor를 null로 넘겨 첫 페이지를 조회한다.
        // PageRequest의 sort는 일부러 지정하지 않는다. 현재 커스텀 Repository 구현이 직접 createdAt desc를 적용하기 때문이다.
        Instant cursor = null;
        Pageable pageable = PageRequest.of(0, 2);
        Slice<Message> allByChannelId = messageRepository.findAllByChannelId(savedChannelId, pageable, cursor);

        // then
        // Slice 메타데이터가 요청한 Pageable을 반영해야 한다.
        assertThat(allByChannelId.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(allByChannelId.getPageable().getPageSize()).isEqualTo(2);

        // 대상 채널에는 pageSize보다 1건 많은 3건이 있다.
        // Repository는 3건을 조회해 다음 페이지가 있음을 감지하지만,
        // Slice content에는 pageSize인 2건만 담아 반환해야 한다.
        assertThat(allByChannelId.getContent()).hasSize(2);
        assertThat(allByChannelId.hasNext()).isTrue();

        // 초과 조회된 savedOldestMessage는 hasNext 판단에만 쓰이고 content에서는 제거되어야 한다.
        // 그래서 content에는 createdAt desc 기준 최신 2건만 남아야 한다.
        assertThat(allByChannelId.getContent())
                .extracting(Message::getId)
                .containsExactly(savedNewestMessageId, savedMiddleMessageId);
        assertThat(allByChannelId.getContent())
                .extracting(Message::getContent)
                .containsExactly(newestMessageCreateCommand.content(), middleMessageCreateCommand.content());
        assertThat(allByChannelId.getContent())
                .extracting(Message::getId)
                .doesNotContain(savedOldestMessageId);
        assertThat(allByChannelId.getContent())
                .allSatisfy(message -> assertThat(message.getChannelId()).isEqualTo(savedChannelId));
    }

    @Test
    @DisplayName("채널별 메시지 Slice 조회 성공 - pageSize 이하이면 hasNext false 반환")
    void findAllByChannelId_returnsSliceWithHasNextFalse_whenResultSizeDoesNotExceedPageSize() throws InterruptedException {
        // given
        // 이 테스트의 대상은 MessageRepositoryCustomImpl.findAllByChannelId(...)의 hasNext=false 경계 조건이다.
        // 현재 구현은 pageSize보다 1건 더 조회한 뒤, 조회 결과 수가 pageSize보다 클 때만 hasNext를 true로 만든다.
        //
        // 따라서 "조회 대상 채널의 메시지 수 == pageSize"인 경계값을 검증해야 한다.
        // 이 경우 조회 결과 수가 pageSize와 같을 뿐 pageSize보다 크지는 않으므로 hasNext는 false여야 한다.
        // 만약 구현이 messages.size() >= pageSize처럼 잘못 판단하면 이 테스트가 실패한다.
        //
        // 대조군 채널에는 메시지를 1건 더 저장한다.
        // 전체 messages 테이블에는 pageSize보다 많은 row가 있더라도,
        // 조회 대상 채널의 결과 수가 pageSize 이하이면 hasNext=false가 되어야 하기 때문이다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고 실제 Entity를 저장한다.
        // Message.author는 nullable이지만 실제 서비스 흐름의 메시지는 작성자를 가지므로 실제 User를 연결한다.
        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        // findAllByChannelId(...)는 메시지 목록을 반환하는 메서드다.
        // 커스텀 Querydsl 구현에서 author, profile, userStatus를 fetch join하므로 실제 연관 row를 함께 둔다.
        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        // Message.channel은 nullable = false 연관관계다.
        // savedChannel은 조회 대상 채널이고, savedOtherChannel은 channel.id 조건 검증을 위한 대조군 채널이다.
        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        ChannelCreatePublicCommand otherChannelCreateCommand = new ChannelCreatePublicCommand(
                "otherPublicChannelName",
                "otherPublicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedOtherChannel = channelRepository.saveAndFlush(new Channel(otherChannelCreateCommand));

        // 대상 채널의 가장 오래된 메시지다.
        // pageSize가 2일 때 content의 두 번째 요소로 반환되어야 한다.
        MessageCreateCommand olderMessageCreateCommand = new MessageCreateCommand(
                "olderMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedOlderMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, olderMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대상 채널의 최신 메시지다.
        // cursor가 null인 첫 페이지에서 content의 첫 번째 요소로 반환되어야 한다.
        MessageCreateCommand newerMessageCreateCommand = new MessageCreateCommand(
                "newerMessageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedNewerMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, newerMessageCreateCommand)
        );

        Thread.sleep(100);

        // 대조군 채널의 메시지다.
        // 전체 테이블 기준으로는 세 번째 메시지지만, channelId가 다르므로 savedChannelId 조회의 hasNext 판단에 영향을 주면 안 된다.
        MessageCreateCommand otherChannelMessageCreateCommand = new MessageCreateCommand(
                "otherChannelMessageContent",
                savedAuthor.getId(),
                savedOtherChannel.getId()
        );
        Message savedOtherChannelMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedOtherChannel, otherChannelMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedOlderMessageId = savedOlderMessage.getId();
        UUID savedNewerMessageId = savedNewerMessage.getId();
        UUID savedOtherChannelMessageId = savedOtherChannelMessage.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();

        // 사전 조건을 먼저 고정한다.
        // 여기서 id나 createdAt 순서가 비정상이면 hasNext=false 검증 실패인지 fixture 구성 실패인지 구분하기 어렵다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedOlderMessageId).isNotNull();
        assertThat(savedNewerMessageId).isNotNull();
        assertThat(savedOtherChannelMessageId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedNewerMessageId).isNotEqualTo(savedOlderMessageId);
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessageId)
                .isNotEqualTo(savedOlderMessageId)
                .isNotEqualTo(savedNewerMessageId);
        assertThat(savedOlderMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedNewerMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
        assertThat(savedOlderMessage.getCreatedAt()).isNotNull();
        assertThat(savedNewerMessage.getCreatedAt()).isNotNull();
        assertThat(savedOtherChannelMessage.getCreatedAt()).isNotNull();
        assertThat(savedNewerMessage.getCreatedAt()).isAfter(savedOlderMessage.getCreatedAt());
        assertThat(savedOtherChannelMessage.getCreatedAt()).isAfter(savedNewerMessage.getCreatedAt());
        assertThat(messageRepository.count()).isEqualTo(3);

        // 저장 직후의 Message가 1차 캐시에 남아 있으면 정렬 쿼리 결과와 캐시 상태를 혼동할 수 있다.
        // 영속성 컨텍스트를 비워 아래 when 절이 실제 Querydsl fetch join, where, order by, limit 결과를 기준으로 검증되게 한다.
        em.clear();

        // when
        // cursor를 null로 넘겨 첫 페이지를 조회한다.
        // PageRequest의 sort는 일부러 지정하지 않는다. 현재 커스텀 Repository 구현이 직접 createdAt desc를 적용하기 때문이다.
        Instant cursor = null;
        Pageable pageable = PageRequest.of(0, 2);
        Slice<Message> allByChannelId = messageRepository.findAllByChannelId(savedChannelId, pageable, cursor);

        // then
        // Slice 메타데이터가 요청한 Pageable을 반영해야 한다.
        assertThat(allByChannelId.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(allByChannelId.getPageable().getPageSize()).isEqualTo(2);

        // 대상 채널에는 pageSize와 정확히 같은 2건만 존재한다.
        // 구현이 limit(pageSize + 1)로 조회하더라도 세 번째 대상 채널 메시지가 없으므로 hasNext는 false여야 한다.
        assertThat(allByChannelId.getContent()).hasSize(2);
        assertThat(allByChannelId.hasNext()).isFalse();

        // 대조군 채널 메시지가 전체 테이블에는 존재하고 더 최신이어도,
        // channelId가 다르므로 savedChannelId 조회 결과와 hasNext 판단에 포함되면 안 된다.
        // 결과적으로 대상 채널의 두 메시지만 createdAt desc 순서로 반환되어야 한다.
        assertThat(allByChannelId.getContent())
                .extracting(Message::getId)
                .containsExactly(savedNewerMessageId, savedOlderMessageId);
        assertThat(allByChannelId.getContent())
                .extracting(Message::getContent)
                .containsExactly(newerMessageCreateCommand.content(), olderMessageCreateCommand.content());
        assertThat(allByChannelId.getContent())
                .allSatisfy(message -> assertThat(message.getChannelId()).isEqualTo(savedChannelId));
    }

    @Test
    @DisplayName("채널별 메시지 Slice 조회 성공 - 채널, 작성자, 프로필, 작성자 상태를 함께 조회")
    void findAllByChannelId_fetchesChannelAuthorProfileAndStatus_whenMessagesExist() {
        // given
        // 이 테스트의 대상은 MessageRepositoryCustomImpl.findAllByChannelId(...)의 fetch join 계약이다.
        // 커스텀 Querydsl 구현은 다음 연관관계를 함께 조회하도록 선언되어 있다.
        //
        // 1. message.channel
        // 2. message.author
        // 3. author.profile
        // 4. author.userStatus
        //
        // 단순히 Message content나 channelId만 확인하면 fetch join이 적용됐는지 검증할 수 없다.
        // 따라서 영속성 컨텍스트를 비운 뒤 다시 조회하고,
        // 연관 객체 getter를 호출하기 전에 PersistenceUnitUtil.isLoaded(...)로 로딩 여부를 확인한다.
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent("test.png", "image/png", 1_024L)
        );

        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고 실제 Entity를 저장한다.
        // author.profile fetch join을 검증하려면 User가 실제 BinaryContent profile을 참조해야 한다.
        UserCreateCommand authorCreateCommand = new UserCreateCommand(
                "testUser",
                "testPassword",
                "test@gmail.com"
        );
        User savedAuthor = userRepository.saveAndFlush(new User(authorCreateCommand, savedProfile));

        // author.userStatus fetch join을 검증하려면 실제 UserStatus row가 필요하다.
        // UserStatus는 User와 mappedBy OneToOne 관계이므로 실제 User를 먼저 저장한 뒤 연결한다.
        Instant lastActiveAt = Instant.now();
        UserStatus savedAuthorStatus = userStatusRepository.saveAndFlush(
                new UserStatus(savedAuthor, new UserStatusCreateCommand(lastActiveAt))
        );

        // Message.channel은 nullable = false 연관관계다.
        // savedChannel은 조회 대상 채널이고, savedOtherChannel은 channelId 조건이 적용되는지 확인하기 위한 대조군 채널이다.
        ChannelCreatePublicCommand channelCreateCommand = new ChannelCreatePublicCommand(
                "publicChannelName",
                "publicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedChannel = channelRepository.saveAndFlush(new Channel(channelCreateCommand));

        ChannelCreatePublicCommand otherChannelCreateCommand = new ChannelCreatePublicCommand(
                "otherPublicChannelName",
                "otherPublicChannelDescription",
                ChannelType.PUBLIC
        );
        Channel savedOtherChannel = channelRepository.saveAndFlush(new Channel(otherChannelCreateCommand));

        // 조회 대상 메시지다.
        // 이 메시지를 다시 조회했을 때 channel, author, author.profile, author.userStatus가 모두 로딩되어 있어야 한다.
        MessageCreateCommand messageCreateCommand = new MessageCreateCommand(
                "messageContent",
                savedAuthor.getId(),
                savedChannel.getId()
        );
        Message savedMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedChannel, messageCreateCommand)
        );

        // 대조군 채널의 메시지다.
        // 이 row가 있어야 messages 테이블에 다른 채널의 메시지가 있어도 savedChannelId 조회 결과에 섞이지 않는다는 점을 함께 확인할 수 있다.
        MessageCreateCommand otherChannelMessageCreateCommand = new MessageCreateCommand(
                "otherChannelMessageContent",
                savedAuthor.getId(),
                savedOtherChannel.getId()
        );
        Message savedOtherChannelMessage = messageRepository.saveAndFlush(
                new Message(savedAuthor, savedOtherChannel, otherChannelMessageCreateCommand)
        );

        UUID savedProfileId = savedProfile.getId();
        UUID savedAuthorId = savedAuthor.getId();
        UUID savedAuthorStatusId = savedAuthorStatus.getId();
        UUID savedMessageId = savedMessage.getId();
        UUID savedOtherChannelMessageId = savedOtherChannelMessage.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();

        // 사전 조건을 먼저 고정한다.
        // 여기서 id가 null이거나 대조군과 조회 대상이 구분되지 않으면 fetch join 검증 실패인지 fixture 구성 실패인지 구분하기 어렵다.
        assertThat(savedProfileId).isNotNull();
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedAuthorStatusId).isNotNull();
        assertThat(savedMessageId).isNotNull();
        assertThat(savedOtherChannelMessageId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessageId).isNotEqualTo(savedMessageId);
        assertThat(savedMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
        assertThat(savedMessage.getCreatedAt()).isNotNull();
        assertThat(savedOtherChannelMessage.getCreatedAt()).isNotNull();
        assertThat(messageRepository.count()).isEqualTo(2);

        // 저장 직후의 Message가 1차 캐시에 남아 있으면 정렬 쿼리 결과와 캐시 상태를 혼동할 수 있다.
        // 영속성 컨텍스트를 비워 아래 when 절이 실제 Querydsl fetch join 결과를 기준으로 검증되게 한다.
        em.clear();

        // when
        // 대상 채널 id로 메시지 목록을 조회한다.
        // PageRequest의 sort는 일부러 지정하지 않는다. 현재 커스텀 Repository 구현이 직접 createdAt desc를 적용하기 때문이다.
        Instant cursor = null;
        Pageable pageable = PageRequest.of(0, 1);
        Slice<Message> allByChannelId = messageRepository.findAllByChannelId(savedChannelId, pageable, cursor);

        // then
        // 조회 대상 채널의 메시지 1건만 반환되어야 한다.
        // 대조군 채널 메시지는 channelId가 다르므로 결과에 포함되면 안 된다.
        assertThat(allByChannelId.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(allByChannelId.getPageable().getPageSize()).isEqualTo(1);
        assertThat(allByChannelId.getContent()).hasSize(1);
        assertThat(allByChannelId.hasNext()).isFalse();

        Message foundMessage = allByChannelId.getContent().get(0);
        PersistenceUnitUtil persistenceUnitUtil = getPersistenceUnitUtil();

        // 연관 객체의 getter를 먼저 호출하면 LAZY 로딩이 뒤늦게 발생할 수 있다.
        // fetch join으로 조회 직후 이미 로딩된 것인지 확인하려면 getter 접근 전에 isLoaded(...)를 먼저 검증해야 한다.
        assertThat(persistenceUnitUtil.isLoaded(foundMessage, "channel")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(foundMessage, "author")).isTrue();

        User author = foundMessage.getAuthor();
        Channel channel = foundMessage.getChannel();

        // author.profile과 author.userStatus도 Querydsl fetch join 대상이다.
        // 이 검증 역시 author.getProfile(), author.getUserStatus() 접근 전에 수행한다.
        assertThat(persistenceUnitUtil.isLoaded(author, "profile")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(author, "userStatus")).isTrue();

        BinaryContent profile = author.getProfile();
        UserStatus userStatus = author.getUserStatus();

        // 조회된 Message가 given에서 저장한 대상 메시지인지 식별자와 주요 필드로 확인한다.
        assertThat(foundMessage.getId()).isEqualTo(savedMessageId);
        assertThat(foundMessage.getContent()).isEqualTo(messageCreateCommand.content());
        assertThat(foundMessage.getChannelId()).isEqualTo(savedChannelId);

        // fetch join으로 함께 조회된 Channel이 실제 저장한 대상 채널인지 확인한다.
        assertThat(channel.getId()).isEqualTo(savedChannelId);
        assertThat(channel.getName()).isEqualTo(channelCreateCommand.channelName());
        assertThat(channel.getDescription()).isEqualTo(channelCreateCommand.channelDescription());
        assertThat(channel.getType()).isEqualTo(channelCreateCommand.channelType());

        // fetch join으로 함께 조회된 User가 실제 작성자인지 확인한다.
        assertThat(author.getId()).isEqualTo(savedAuthorId);
        assertThat(author.getUsername()).isEqualTo(authorCreateCommand.username());
        assertThat(author.getEmail()).isEqualTo(authorCreateCommand.email());

        // fetch join으로 함께 조회된 profile이 실제 BinaryContent row인지 확인한다.
        assertThat(profile.getId()).isEqualTo(savedProfileId);
        assertThat(profile.getOriginalFileName()).isEqualTo(savedProfile.getOriginalFileName());
        assertThat(profile.getContentType()).isEqualTo(savedProfile.getContentType());
        assertThat(profile.getSize()).isEqualTo(savedProfile.getSize());
        assertThat(author.getProfileId()).isEqualTo(savedProfileId);

        // fetch join으로 함께 조회된 UserStatus가 실제 작성자 상태 row인지 확인한다.
        assertThat(userStatus.getId()).isEqualTo(savedAuthorStatusId);
        assertThat(userStatus.getUserId()).isEqualTo(savedAuthorId);
        assertThat(Duration.between(lastActiveAt, userStatus.getLastActiveAt()).abs())
                .isLessThanOrEqualTo(Duration.ofNanos(1_000));
    }

    @Test
    @DisplayName("채널별 메시지 Slice 조회 실패 - channelId가 null이면 데이터 접근 예외 발생")
    void findAllByChannelId_throwsDataAccessException_whenChannelIdIsNull() {
        // given
        // 이 테스트의 대상은 MessageRepositoryCustomImpl.findAllByChannelId(...)의 입력 검증이다.
        // 구현의 filterByChannelId(...)는 channelId가 null이면 IllegalArgumentException을 던진다.
        // 다만 Repository Bean에는 Spring 예외 변환이 적용되므로,
        // 테스트에서 관찰되는 최종 예외 타입은 InvalidDataAccessApiUsageException이다.
        //
        // 여기서는 DB row가 필요한 검증이 아니다.
        // Pageable은 정상 값으로 넘기고 channelId만 null로 둬, 실패 원인이 channelId 검증임을 분명히 한다.
        UUID channelId = null;
        Pageable pageable = PageRequest.of(0, 2);
        Instant cursor = null;

        // when / then
        // channelId가 null이면 Querydsl where 조건을 만들 수 없으므로 데이터 접근 예외가 발생해야 한다.
        // 겉으로 드러나는 예외는 Spring이 변환한 InvalidDataAccessApiUsageException이고,
        // root cause는 구현이 직접 던진 IllegalArgumentException이어야 한다.
        assertThatThrownBy(() -> messageRepository.findAllByChannelId(channelId, pageable, cursor))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("channelId must not be null")
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("channelId must not be null");
    }

    @Test
    @DisplayName("채널 기준 메시지 존재 여부 조회 성공 - 채널에 메시지가 있으면 true 반환")
    void existsByChannel_Id_returnsTrue_whenChannelHasMessages() {
        // given
        // 이 테스트의 대상은 Spring Data JPA derived query인 MessageRepository.existsByChannel_Id(...)다.
        // Repository 테스트이므로 실제 User, Channel, Message 엔티티를 테스트 DB에 저장한 뒤,
        // channel_id 조건으로 메시지 존재 여부를 판단하는지 확인한다.
        User savedAuthor = saveUserWithProfileAndStatus("channelMessageAuthor", "channel-message-author@gmail.com");
        Channel savedChannel = savePublicChannel("channelWithMessages");
        Channel savedOtherChannel = savePublicChannel("channelWithoutMessagesForExistsTrue");
        Message savedMessage = saveMessage(savedAuthor, savedChannel, "channelMessageContent");

        UUID savedAuthorId = savedAuthor.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();
        UUID savedMessageId = savedMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 저장된 메시지가 실제로 savedChannel을 참조하고, 대조군 채널은 다른 id를 가져야
        // existsByChannel_Id(...)가 단순히 messages 테이블 전체 존재 여부를 보는 쿼리가 아님을 확인할 수 있다.
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedMessageId).isNotNull();
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(messageRepository.count()).isEqualTo(1);

        // 저장 직후의 Message가 1차 캐시에 남아 있어도 exists 쿼리에는 직접 영향이 크지 않지만,
        // Repository 테스트에서는 실제 DB row 기준으로 판단한다는 의도를 명확히 하기 위해 영속성 컨텍스트를 비운다.
        em.clear();

        // when
        // 메시지가 연결된 채널 id로 존재 여부를 조회한다.
        boolean exists = messageRepository.existsByChannel_Id(savedChannelId);

        // then
        // messages 테이블에 savedChannelId를 가진 row가 있으므로 true가 반환되어야 한다.
        // 반대로 메시지가 연결되지 않은 대조군 채널은 실제로 존재하더라도 false여야 한다.
        assertThat(exists).isTrue();
        assertThat(messageRepository.existsByChannel_Id(savedOtherChannelId)).isFalse();
    }

    @Test
    @DisplayName("채널 기준 메시지 존재 여부 조회 성공 - 채널에 메시지가 없으면 false 반환")
    void existsByChannel_Id_returnsFalse_whenChannelHasNoMessages() {
        // given
        // 이 테스트는 "채널은 존재하지만 그 채널에 연결된 메시지는 없는 경우"를 검증한다.
        // 임의 UUID나 빈 messages 테이블만으로 false를 확인하면 channel_id 조건이 실제로 적용됐는지 알기 어렵다.
        //
        // 그래서 조회 대상 채널은 실제로 저장하되 메시지는 연결하지 않고,
        // 다른 채널에는 메시지를 저장해 messages 테이블이 비어 있지 않은 상황을 만든다.
        User savedAuthor = saveUserWithProfileAndStatus("otherChannelMessageAuthor", "other-channel-message-author@gmail.com");
        Channel savedChannel = savePublicChannel("channelWithoutMessages");
        Channel savedOtherChannel = savePublicChannel("otherChannelWithMessages");
        Message savedOtherChannelMessage = saveMessage(savedAuthor, savedOtherChannel, "otherChannelMessageContent");

        UUID savedAuthorId = savedAuthor.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();
        UUID savedOtherChannelMessageId = savedOtherChannelMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 대조군 채널에는 메시지가 있고, 조회 대상 채널에는 메시지가 없어야 false 검증의 의미가 분명해진다.
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedOtherChannelMessageId).isNotNull();
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
        assertThat(messageRepository.count()).isEqualTo(1);

        // exists 조회가 저장 직후 관리 객체가 아니라 DB row를 기준으로 수행되게 한다.
        em.clear();

        // when
        // DB에 존재하지만 메시지가 하나도 연결되지 않은 채널 id로 존재 여부를 조회한다.
        boolean exists = messageRepository.existsByChannel_Id(savedChannelId);

        // then
        // 다른 채널에는 메시지가 있어도 savedChannelId와 일치하는 row는 없으므로 false가 반환되어야 한다.
        assertThat(messageRepository.existsByChannel_Id(savedOtherChannelId)).isTrue();
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("채널별 메시지 전체 삭제 성공 - 채널에 연결된 메시지 삭제")
    void deleteAllByChannel_Id_deletesMessages_whenChannelHasMessages() {
        // given
        // 이 테스트의 대상은 MessageRepository.deleteAllByChannel_Id(...)다.
        // 삭제 메서드는 channel.id 조건과 일치하는 메시지만 삭제해야 하고,
        // 다른 채널에 연결된 메시지는 그대로 남겨야 한다.
        User savedAuthor = saveUserWithProfileAndStatus("deleteMessageAuthor", "delete-message-author@gmail.com");
        Channel savedChannel = savePublicChannel("deleteTargetChannel");
        Channel savedOtherChannel = savePublicChannel("deleteOtherChannel");
        Message savedMessage = saveMessage(savedAuthor, savedChannel, "deleteTargetMessageContent");
        Message savedOtherMessage = saveMessage(savedAuthor, savedChannel, "deleteTargetOtherMessageContent");
        Message savedOtherChannelMessage = saveMessage(savedAuthor, savedOtherChannel, "deleteOtherChannelMessageContent");

        UUID savedAuthorId = savedAuthor.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherChannelId = savedOtherChannel.getId();
        UUID savedMessageId = savedMessage.getId();
        UUID savedOtherMessageId = savedOtherMessage.getId();
        UUID savedOtherChannelMessageId = savedOtherChannelMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 대상 채널에는 2건, 대조군 채널에는 1건이 있어야 삭제 범위 검증이 가능하다.
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotNull();
        assertThat(savedOtherChannelId).isNotEqualTo(savedChannelId);
        assertThat(savedMessageId).isNotNull();
        assertThat(savedOtherMessageId).isNotNull();
        assertThat(savedOtherChannelMessageId).isNotNull();
        assertThat(savedOtherMessageId).isNotEqualTo(savedMessageId);
        assertThat(savedOtherChannelMessageId)
                .isNotEqualTo(savedMessageId)
                .isNotEqualTo(savedOtherMessageId);
        assertThat(savedMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherChannelMessage.getChannelId()).isEqualTo(savedOtherChannelId);
        assertThat(messageRepository.count()).isEqualTo(3);

        // 삭제 전 조회가 DB 기준으로 수행되게 영속성 컨텍스트를 비운다.
        em.clear();
        assertThat(messageRepository.existsByChannel_Id(savedChannelId)).isTrue();
        assertThat(messageRepository.existsByChannel_Id(savedOtherChannelId)).isTrue();

        // when
        // 대상 채널에 연결된 메시지를 모두 삭제한다.
        messageRepository.deleteAllByChannel_Id(savedChannelId);
        messageRepository.flush();
        em.clear();

        // then
        // savedChannel에 연결됐던 메시지 2건은 삭제되어야 한다.
        // 이 메서드는 메시지 삭제 메서드이므로 Channel row 자체를 삭제하면 안 된다.
        assertThat(channelRepository.findById(savedChannelId)).isPresent();
        assertThat(messageRepository.findById(savedMessageId)).isEmpty();
        assertThat(messageRepository.findById(savedOtherMessageId)).isEmpty();
        assertThat(messageRepository.findTop1ByChannel_IdOrderByCreatedAtDesc(savedChannelId)).isEmpty();
        assertThat(messageRepository.existsByChannel_Id(savedChannelId)).isFalse();

        // 대조군 채널의 메시지는 삭제 대상이 아니므로 그대로 남아 있어야 한다.
        // 대조군 Channel row도 함께 남아 있어야 delete 조건이 channel_id에 정확히 제한됐다고 볼 수 있다.
        assertThat(channelRepository.findById(savedOtherChannelId)).isPresent();
        assertThat(messageRepository.findById(savedOtherChannelMessageId))
                .hasValueSatisfying(foundMessage -> {
                    assertThat(foundMessage.getId()).isEqualTo(savedOtherChannelMessageId);
                    assertThat(foundMessage.getContent()).isEqualTo("deleteOtherChannelMessageContent");
                    assertThat(foundMessage.getChannelId()).isEqualTo(savedOtherChannelId);
                });
        assertThat(messageRepository.existsByChannel_Id(savedOtherChannelId)).isTrue();
        assertThat(messageRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("작성자 기준 메시지 존재 여부 조회 성공 - 작성자의 메시지가 있으면 true 반환")
    void existsByAuthor_Id_returnsTrue_whenAuthorHasMessages() {
        // given
        // 이 테스트의 대상은 MessageRepository.existsByAuthor_Id(...)다.
        // 실제 작성자와 메시지를 저장해 author_id 조건으로 존재 여부를 판단하는지 확인한다.
        User savedAuthor = saveUserWithProfileAndStatus("authorWithMessages", "author-with-messages@gmail.com");
        User savedOtherAuthor = saveUserWithProfileAndStatus("authorWithoutMessagesForExistsTrue", "author-without-messages-for-exists-true@gmail.com");
        Channel savedChannel = savePublicChannel("authorExistsChannel");
        Message savedMessage = saveMessage(savedAuthor, savedChannel, "authorMessageContent");

        UUID savedAuthorId = savedAuthor.getId();
        UUID savedOtherAuthorId = savedOtherAuthor.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedMessageId = savedMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 메시지가 실제 savedAuthor와 savedChannel을 참조하고, 대조군 작성자는 다른 id를 가져야
        // existsByAuthor_Id(...)가 단순 메시지 존재 여부가 아니라 author_id 조건을 기준으로 판단하는지 확인할 수 있다.
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedOtherAuthorId).isNotNull();
        assertThat(savedOtherAuthorId).isNotEqualTo(savedAuthorId);
        assertThat(savedChannelId).isNotNull();
        assertThat(savedMessageId).isNotNull();
        assertThat(savedMessage.getAuthor().getId()).isEqualTo(savedAuthorId);
        assertThat(savedMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(messageRepository.count()).isEqualTo(1);

        // 실제 DB row 기준으로 exists 쿼리를 검증하기 위해 영속성 컨텍스트를 비운다.
        em.clear();

        // when
        // 메시지를 작성한 사용자 id로 존재 여부를 조회한다.
        boolean exists = messageRepository.existsByAuthor_Id(savedAuthorId);

        // then
        // messages 테이블에 savedAuthorId를 가진 row가 있으므로 true가 반환되어야 한다.
        // 반대로 메시지를 작성하지 않은 대조군 작성자는 실제로 존재하더라도 false여야 한다.
        assertThat(exists).isTrue();
        assertThat(messageRepository.existsByAuthor_Id(savedOtherAuthorId)).isFalse();
    }

    @Test
    @DisplayName("작성자 기준 메시지 존재 여부 조회 성공 - 작성자의 메시지가 없으면 false 반환")
    void existsByAuthor_Id_returnsFalse_whenAuthorHasNoMessages() {
        // given
        // 이 테스트는 "사용자는 존재하지만 그 사용자가 작성한 메시지는 없는 경우"를 검증한다.
        // 빈 messages 테이블에서 false를 확인하면 author_id 조건이 적용됐는지 알기 어렵다.
        //
        // 그래서 조회 대상 작성자는 실제로 저장하되 메시지는 연결하지 않고,
        // 다른 작성자에게 메시지를 저장해 messages 테이블이 비어 있지 않은 상황을 만든다.
        User savedAuthor = saveUserWithProfileAndStatus("authorWithoutMessages", "author-without-messages@gmail.com");
        User savedOtherAuthor = saveUserWithProfileAndStatus("otherAuthorWithMessages", "other-author-with-messages@gmail.com");
        Channel savedChannel = savePublicChannel("authorFalseChannel");
        Message savedOtherAuthorMessage = saveMessage(savedOtherAuthor, savedChannel, "otherAuthorMessageContent");

        UUID savedAuthorId = savedAuthor.getId();
        UUID savedOtherAuthorId = savedOtherAuthor.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedOtherAuthorMessageId = savedOtherAuthorMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 대조군 작성자에게는 메시지가 있고, 조회 대상 작성자에게는 메시지가 없어야 false 검증이 분명해진다.
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedOtherAuthorId).isNotNull();
        assertThat(savedChannelId).isNotNull();
        assertThat(savedOtherAuthorMessageId).isNotNull();
        assertThat(savedOtherAuthorId).isNotEqualTo(savedAuthorId);
        assertThat(savedOtherAuthorMessage.getAuthor().getId()).isEqualTo(savedOtherAuthorId);
        assertThat(savedOtherAuthorMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(messageRepository.count()).isEqualTo(1);

        // 실제 DB row 기준으로 exists 쿼리를 검증하기 위해 영속성 컨텍스트를 비운다.
        em.clear();

        // when
        // DB에 존재하지만 메시지를 작성하지 않은 사용자 id로 존재 여부를 조회한다.
        boolean exists = messageRepository.existsByAuthor_Id(savedAuthorId);

        // then
        // 다른 작성자에게 메시지가 있어도 savedAuthorId와 일치하는 row는 없으므로 false가 반환되어야 한다.
        assertThat(messageRepository.existsByAuthor_Id(savedOtherAuthorId)).isTrue();
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("작성자 연결 해제 성공 - 작성자의 메시지 author를 null로 변경")
    void detachAuthorByAuthorId_setsAuthorNull_whenAuthorHasMessages() {
        // given
        // 이 테스트의 대상은 MessageRepository.detachAuthorByAuthorId(...) 벌크 update 쿼리다.
        // 특정 작성자가 탈퇴하거나 삭제될 때, 그 작성자의 메시지는 남기되 author 참조만 null로 끊어야 한다.
        //
        // 대상 작성자의 메시지 2건과 대조군 작성자의 메시지 1건을 저장해,
        // update 범위가 author_id 조건으로 정확히 제한되는지 확인한다.
        User savedAuthor = saveUserWithProfileAndStatus("detachTargetAuthor", "detach-target-author@gmail.com");
        User savedOtherAuthor = saveUserWithProfileAndStatus("detachOtherAuthor", "detach-other-author@gmail.com");
        Channel savedChannel = savePublicChannel("detachChannel");
        Message savedMessage = saveMessage(savedAuthor, savedChannel, "detachMessageContent");
        Message savedOtherMessage = saveMessage(savedAuthor, savedChannel, "detachOtherMessageContent");
        Message savedOtherAuthorMessage = saveMessage(savedOtherAuthor, savedChannel, "otherAuthorRemainMessageContent");

        UUID savedAuthorId = savedAuthor.getId();
        UUID savedOtherAuthorId = savedOtherAuthor.getId();
        UUID savedChannelId = savedChannel.getId();
        UUID savedMessageId = savedMessage.getId();
        UUID savedOtherMessageId = savedOtherMessage.getId();
        UUID savedOtherAuthorMessageId = savedOtherAuthorMessage.getId();

        // 사전 조건을 먼저 고정한다.
        // 대상 작성자 메시지와 대조군 작성자 메시지가 명확히 구분되어야 update 범위 검증이 가능하다.
        assertThat(savedAuthorId).isNotNull();
        assertThat(savedOtherAuthorId).isNotNull();
        assertThat(savedOtherAuthorId).isNotEqualTo(savedAuthorId);
        assertThat(savedChannelId).isNotNull();
        assertThat(savedMessageId).isNotNull();
        assertThat(savedOtherMessageId).isNotNull();
        assertThat(savedOtherAuthorMessageId).isNotNull();
        assertThat(savedOtherMessageId).isNotEqualTo(savedMessageId);
        assertThat(savedOtherAuthorMessageId)
                .isNotEqualTo(savedMessageId)
                .isNotEqualTo(savedOtherMessageId);
        assertThat(savedMessage.getAuthor().getId()).isEqualTo(savedAuthorId);
        assertThat(savedOtherMessage.getAuthor().getId()).isEqualTo(savedAuthorId);
        assertThat(savedOtherAuthorMessage.getAuthor().getId()).isEqualTo(savedOtherAuthorId);
        assertThat(savedMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(savedOtherAuthorMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(messageRepository.count()).isEqualTo(3);

        // 벌크 update는 영속성 컨텍스트에 이미 올라온 엔티티 상태를 자동으로 갱신하지 않는다.
        // 따라서 실행 전후로 clear해서 사전 조회와 사후 조회 모두 DB row 기준이 되게 한다.
        em.clear();
        assertThat(messageRepository.existsByAuthor_Id(savedAuthorId)).isTrue();
        assertThat(messageRepository.existsByAuthor_Id(savedOtherAuthorId)).isTrue();

        // when
        // 대상 작성자의 메시지 author 참조를 null로 변경한다.
        messageRepository.detachAuthorByAuthorId(savedAuthorId);
        messageRepository.flush();
        em.clear();

        // then
        // 대상 작성자의 메시지들은 삭제되지 않고 남아 있어야 하며, author만 null이어야 한다.
        // 벌크 update 후에는 영속성 컨텍스트를 비우고 다시 조회해야 DB에 실제 반영된 FK 값을 볼 수 있다.
        Message foundMessage = messageRepository.findById(savedMessageId).orElseThrow(AssertionError::new);
        Message foundOtherMessage = messageRepository.findById(savedOtherMessageId).orElseThrow(AssertionError::new);
        assertThat(foundMessage.getAuthor()).isNull();
        assertThat(foundOtherMessage.getAuthor()).isNull();
        assertThat(foundMessage.getContent()).isEqualTo("detachMessageContent");
        assertThat(foundOtherMessage.getContent()).isEqualTo("detachOtherMessageContent");
        assertThat(foundMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(foundOtherMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(messageRepository.existsByAuthor_Id(savedAuthorId)).isFalse();

        // 대조군 작성자의 메시지는 update 대상이 아니므로 author가 그대로 유지되어야 한다.
        // 같은 채널에 있는 메시지라도 author_id가 다르면 FK가 null로 바뀌면 안 된다.
        Message foundOtherAuthorMessage = messageRepository.findById(savedOtherAuthorMessageId)
                .orElseThrow(AssertionError::new);
        assertThat(foundOtherAuthorMessage.getAuthor()).isNotNull();
        assertThat(foundOtherAuthorMessage.getAuthor().getId()).isEqualTo(savedOtherAuthorId);
        assertThat(foundOtherAuthorMessage.getContent()).isEqualTo("otherAuthorRemainMessageContent");
        assertThat(foundOtherAuthorMessage.getChannelId()).isEqualTo(savedChannelId);
        assertThat(messageRepository.existsByAuthor_Id(savedOtherAuthorId)).isTrue();
        assertThat(messageRepository.count()).isEqualTo(3);
    }

    private void assertFetchedMessageFixture(
            Message foundMessage,
            UUID expectedMessageId,
            MessageCreateCommand expectedMessageCommand,
            UUID expectedAuthorId,
            UserCreateCommand expectedAuthorCommand,
            UUID expectedAuthorStatusId,
            BinaryContent expectedProfile,
            ChannelCreatePublicCommand expectedChannelCommand,
            UUID expectedChannelId
    ) {
        PersistenceUnitUtil persistenceUnitUtil = getPersistenceUnitUtil();

        // EntityGraph에 직접 선언된 Message.author와 Message.channel이 조회 직후 로딩되어 있어야 한다.
        // 이 검증은 getAuthor(), getChannel() 접근 전에 수행해야 지연 로딩으로 인한 오탐을 줄일 수 있다.
        assertThat(persistenceUnitUtil.isLoaded(foundMessage, "author")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(foundMessage, "channel")).isTrue();

        User author = foundMessage.getAuthor();
        Channel channel = foundMessage.getChannel();

        // EntityGraph에 중첩 선언된 author.userStatus와 author.profile도 조회 직후 로딩되어 있어야 한다.
        // UserStatus는 User의 mappedBy OneToOne 관계이고, profile은 User가 직접 참조하는 OneToOne 관계다.
        assertThat(persistenceUnitUtil.isLoaded(author, "userStatus")).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(author, "profile")).isTrue();

        BinaryContent profile = author.getProfile();
        UserStatus userStatus = author.getUserStatus();

        // Message 자체가 요청한 id의 row인지 확인한다.
        assertThat(foundMessage.getId()).isEqualTo(expectedMessageId);
        assertThat(foundMessage.getContent()).isEqualTo(expectedMessageCommand.content());

        // 작성자 정보가 의도한 User row와 일치하는지 확인한다.
        assertThat(author.getId()).isEqualTo(expectedAuthorId);
        assertThat(author.getUsername()).isEqualTo(expectedAuthorCommand.username());
        assertThat(author.getEmail()).isEqualTo(expectedAuthorCommand.email());

        // 작성자 상태가 함께 조회된 실제 UserStatus row인지 확인한다.
        assertThat(userStatus.getId()).isEqualTo(expectedAuthorStatusId);
        assertThat(userStatus.getUserId()).isEqualTo(expectedAuthorId);

        // 작성자 프로필이 함께 조회된 실제 BinaryContent row인지 확인한다.
        assertThat(profile.getId()).isEqualTo(expectedProfile.getId());
        assertThat(profile.getOriginalFileName()).isEqualTo(expectedProfile.getOriginalFileName());
        assertThat(profile.getContentType()).isEqualTo(expectedProfile.getContentType());
        assertThat(profile.getSize()).isEqualTo(expectedProfile.getSize());
        assertThat(author.getProfileId()).isEqualTo(expectedProfile.getId());

        // 채널 정보가 의도한 Channel row와 일치하는지 확인한다.
        assertThat(channel.getId()).isEqualTo(expectedChannelId);
        assertThat(channel.getName()).isEqualTo(expectedChannelCommand.channelName());
        assertThat(channel.getDescription()).isEqualTo(expectedChannelCommand.channelDescription());
        assertThat(channel.getType()).isEqualTo(expectedChannelCommand.channelType());
    }

    private void assertMessageDetails(UUID savedMessageId, MessageCreateCommand messageCreateCommand, UUID savedChannelId) {
        assertThat(messageRepository.findById(savedMessageId))
                .hasValueSatisfying(foundMessage -> {
                    assertThat(foundMessage.getId()).isEqualTo(savedMessageId);
                    assertThat(foundMessage.getContent()).isEqualTo(messageCreateCommand.content());
                    assertThat(foundMessage.getChannelId()).isEqualTo(savedChannelId);
                });
    }

    // MessageRepository는 author, author.profile, author.userStatus를 함께 조회하는 메서드를 가진다.
    // 따라서 단순히 User만 저장하지 않고 실제 BinaryContent와 UserStatus까지 함께 저장해
    // Repository 쿼리가 실제 서비스 데이터 형태에 가까운 row를 대상으로 실행되게 한다.
    private User saveUserWithProfileAndStatus(String username, String email) {
        BinaryContent savedProfile = binaryContentRepository.saveAndFlush(
                new BinaryContent(username + ".png", "image/png", 1_024L)
        );
        User savedUser = userRepository.saveAndFlush(
                new User(new UserCreateCommand(username, "testPassword", email), savedProfile)
        );
        userStatusRepository.saveAndFlush(
                new UserStatus(savedUser, new UserStatusCreateCommand(Instant.now()))
        );

        return savedUser;
    }

    // Message.channel은 nullable = false 연관관계다.
    // 메시지 Repository 테스트에서는 채널 존재 여부와 channel_id 조건을 자주 구분해야 하므로
    // 테스트마다 실제 PUBLIC Channel row를 저장해 명확한 조회 대상과 대조군을 만든다.
    private Channel savePublicChannel(String channelName) {
        return channelRepository.saveAndFlush(
                new Channel(new ChannelCreatePublicCommand(
                        channelName,
                        channelName + "Description",
                        ChannelType.PUBLIC
                ))
        );
    }

    // MessageCreateCommand에는 authorId와 channelId가 들어가지만,
    // Repository 테스트에서는 FK 값만 흉내 내지 않고 실제 User, Channel 엔티티를 연결한다.
    // 그래야 save, find, exists, delete, bulk update가 모두 실제 JPA 연관관계 매핑을 통해 검증된다.
    private Message saveMessage(User author, Channel channel, String content) {
        return messageRepository.saveAndFlush(
                new Message(author, channel, new MessageCreateCommand(
                        content,
                        author.getId(),
                        channel.getId()
                ))
        );
    }

    private PersistenceUnitUtil getPersistenceUnitUtil() {
        return em.getEntityManagerFactory().getPersistenceUnitUtil();
    }


}
