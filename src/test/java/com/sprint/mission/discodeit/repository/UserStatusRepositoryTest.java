package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.config.JpaAuditingTestConfig;
import com.sprint.mission.discodeit.config.P6SpySqlFormatter;
import com.sprint.mission.discodeit.config.QuerydslTestConfig;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@Import(value = {QuerydslTestConfig.class, JpaAuditingTestConfig.class, P6SpySqlFormatter.class})
@DisplayName("UserStatusRepository 슬라이스 테스트")
class UserStatusRepositoryTest {

    @Autowired
    UserStatusRepository userStatusRepository;
    @Autowired
    EntityManager em;
    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("사용자 상태 단건 조회 성공 - 상태 ID와 사용자 ID가 모두 일치하면 User를 함께 조회")
    void findByIdAndUserId_fetchesUser_whenIdAndUserIdMatch() {
        // given
        // 이 테스트의 대상은 UserStatusRepository.findByIdAndUserId(...) 쿼리다.
        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고, 실제 User와 UserStatus 엔티티를
        // H2 테스트 DB에 저장한 뒤 repository 메서드가 DB row를 올바르게 조회하는지 검증한다.
        UserCreateCommand userCreateCommand = userCreateCommand();
        UserStatusCreateCommand userStatusCreateCommand = userStatusCreateCommand();

        // UserStatus.user는 nullable = false인 연관관계다.
        // 따라서 상태만 단독으로 만들지 않고 먼저 실제 User를 저장한 다음,
        // 저장된 User를 참조하는 실제 UserStatus를 저장한다.
        UserStatusFixture fixture = saveUserWithStatus(userCreateCommand, userStatusCreateCommand);

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있는 객체가 반환될 수 없다.
        // 즉, 아래 when 절의 결과는 1차 캐시가 아니라 실제 SELECT 결과라는 점이 분명해진다.
        UUID savedUserId = fixture.user().getId();
        UUID savedUserStatusId = fixture.userStatus().getId();
        em.clear();

        // when
        // 상태 ID와 사용자 ID가 모두 일치하는 조건으로 단건 조회한다.
        // findByIdAndUserId(...)는 @Query로 상태 id와 user.id를 함께 제한하고,
        // @EntityGraph(attributePaths = {"user"})로 User를 함께 조회하도록 선언되어 있다.
        UserStatus userStatus = userStatusRepository.findByIdAndUserId(savedUserStatusId, savedUserId)
                .orElseThrow(AssertionError::new);

        // then
        // 먼저 식별자를 확인해 요청한 UserStatus row가 정확히 반환됐는지 검증한다.
        assertThat(userStatus.getId()).isEqualTo(savedUserStatusId);

        // lastActiveAt은 UserStatusCreateCommand에서 온 값이다.
        // 이 값을 함께 확인하면 "아무 UserStatus나 조회됐다"가 아니라
        // given에서 저장한 상태 row가 그대로 조회됐다는 근거가 더 명확해진다.
        //
        // 단, H2의 timestamp(6) 컬럼은 마이크로초 단위까지만 저장한다.
        // Instant가 가진 나노초 값은 DB 저장/조회 과정에서 반올림될 수 있으므로,
        // 정확한 나노초 동일성 대신 DB 정밀도 수준의 오차만 허용한다.
        assertThat(Duration.between(userStatusCreateCommand.createdAt(), userStatus.getLastActiveAt()).abs())
                .isLessThanOrEqualTo(Duration.ofNanos(1_000));

        // @EntityGraph 검증은 userStatus.getUser() 또는 getUserId() 같은 getter 접근 전에 해야 한다.
        // getter를 먼저 호출하면 LAZY 연관관계가 그 시점에 초기화될 수 있어,
        // EntityGraph 때문에 미리 로딩된 것인지 구분하기 어려워진다.
        PersistenceUnitUtil persistenceUnitUtil = getPersistenceUnitUtil();
        assertThat(persistenceUnitUtil.isLoaded(userStatus, "user")).isTrue();

        // 조회 조건에 포함된 사용자 ID도 검증한다.
        // 기존 테스트는 getUserId()를 두 번 비교하고 있었기 때문에,
        // 중복 검증 하나를 제거하고 상태 id 검증과 사용자 id 검증으로 역할을 나눴다.
        assertThat(userStatus.getUserId()).isEqualTo(savedUserId);

        // User가 함께 로딩됐으므로, 연관된 User의 주요 필드도 저장한 command 값과 일치해야 한다.
        // 이 검증은 user_id만 맞는지보다 한 단계 더 나아가 실제 User 엔티티가 올바르게 연결됐는지 확인한다.
        User foundUser = userStatus.getUser();
        assertThat(foundUser.getId()).isEqualTo(savedUserId);
        assertThat(foundUser.getUsername()).isEqualTo(userCreateCommand.username());
        assertThat(foundUser.getEmail()).isEqualTo(userCreateCommand.email());
    }

    @Test
    @DisplayName("사용자 상태 단건 조회 성공 - 사용자 ID가 일치하지 않으면 Optional.empty 반환")
    void findByIdAndUserId_returnsEmpty_whenUserIdDoesNotMatch() {
        // given
        // 이 테스트의 대상은 UserStatusRepository.findByIdAndUserId(...) 쿼리다.
        // 성공 케이스와 달리 여기서는 "상태 ID는 존재하지만 사용자 ID 조건이 틀린 경우"를 검증한다.
        // 따라서 빈 DB에서 Optional.empty가 나오는지만 확인하면 테스트 의미가 약해진다.
        // 실제 UserStatus row를 저장한 뒤, 그 상태의 소유자가 아닌 다른 실제 User의 id로 조회한다.
        UserCreateCommand userCreateCommand = userCreateCommand();
        UserCreateCommand otherUserCreateCommand = userCreateCommand(
                "otherUsername",
                "otherPassword",
                "otherEmail@gmail.com"
        );
        UserStatusCreateCommand userStatusCreateCommand = userStatusCreateCommand();

        // UserStatus.user는 nullable = false이고 users.id를 참조한다.
        // 먼저 상태의 실제 소유자인 User를 저장한 뒤, 그 User에 연결된 UserStatus를 저장한다.
        UserStatusFixture fixture = saveUserWithStatus(userCreateCommand, userStatusCreateCommand);

        // 조회 조건으로 사용할 "틀린 사용자 ID"도 임의 UUID가 아니라 실제 저장된 다른 사용자 id를 사용한다.
        // 이렇게 하면 결과가 비어 있는 이유가 "사용자가 존재하지 않아서"가 아니라
        // "상태 ID와 사용자 ID의 조합이 맞지 않아서"임을 더 정확하게 검증할 수 있다.
        User savedOtherUser = saveUser(otherUserCreateCommand);

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있는 객체가 반환될 수 없다.
        // 즉, 아래 when 절의 결과는 1차 캐시가 아니라 실제 SELECT 결과라는 점이 분명해진다.
        UUID savedUserId = fixture.user().getId();
        UUID savedUserStatusId = fixture.userStatus().getId();
        UUID savedOtherUserId = savedOtherUser.getId();
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // 상태 ID는 실제로 존재하고, 조회에 사용할 사용자 ID는 상태 소유자의 ID와 달라야 한다.
        assertThat(savedUserStatusId).isNotNull();
        assertThat(savedUserId).isNotNull();
        assertThat(savedOtherUserId)
                .isNotNull()
                .isNotEqualTo(savedUserId);

        // when
        // 존재하는 UserStatus의 id를 넘기되, userId에는 다른 사용자의 id를 넘긴다.
        // findByIdAndUserId(...)의 WHERE 조건은 상태 id와 user.id를 모두 만족해야 하므로
        // 이 조합은 조회 대상이 없어야 한다.
        Optional<UserStatus> userStatus = userStatusRepository.findByIdAndUserId(savedUserStatusId, savedOtherUserId);

        // then
        // 같은 상태 ID를 가진 row가 DB에 있더라도 사용자 ID 조건이 다르면 반환하면 안 된다.
        // 이 검증은 사용자가 다른 사람의 UserStatus를 상태 ID만으로 조회할 수 없는지 확인하는 소유권 조건 검증이다.
        assertThat(userStatus).isEmpty();
    }

    @Test
    @DisplayName("사용자별 상태 조회 성공 - 사용자 ID가 일치하면 UserStatus 반환")
    void findByUserId_returnsUserStatus_whenUserStatusExists() {
        // given
        // 이 테스트의 대상은 UserStatusRepository.findByUserId(...) 쿼리다.
        // Repository 슬라이스 테스트이므로 UserStatusRepository, UserRepository, EntityManager는
        // 실제 Spring Data JPA 구성으로 동작하고, User와 UserStatus도 실제 엔티티로 생성한다.
        //
        // 단순히 UserStatus를 하나만 저장하면 userId 조건이 잘못 빠져도 테스트가 우연히 통과할 수 있다.
        // 그래서 조회 대상 사용자와 다른 사용자를 각각 저장하고, 두 사용자 모두 UserStatus를 갖게 만든다.
        // 그러면 findByUserId(...)가 전달받은 userId 조건으로 정확한 row를 고르는지 확인할 수 있다.
        UserCreateCommand userCreateCommand = userCreateCommand();
        UserCreateCommand otherUserCreateCommand = userCreateCommand(
                "otherUsername",
                "otherPassword",
                "otherEmail@gmail.com"
        );
        UserStatusCreateCommand userStatusCreateCommand = userStatusCreateCommand();
        UserStatusCreateCommand otherUserStatusCreateCommand = userStatusCreateCommand(
                userStatusCreateCommand.createdAt().minusSeconds(60)
        );

        // UserStatus.user는 nullable = false인 @OneToOne 연관관계이고 user_id에 unique 제약이 있다.
        // 따라서 각 UserStatus는 먼저 저장된 서로 다른 User를 참조해야 한다.
        UserStatusFixture fixture = saveUserWithStatus(userCreateCommand, userStatusCreateCommand);
        UserStatusFixture otherFixture = saveUserWithStatus(otherUserCreateCommand, otherUserStatusCreateCommand);

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있는 객체가 반환될 수 없다.
        // 즉, 아래 when 절의 결과는 1차 캐시가 아니라 실제 SELECT 결과라는 점이 분명해진다.
        UUID savedUserId = fixture.user().getId();
        UUID savedOtherUserId = otherFixture.user().getId();
        UUID savedUserStatusId = fixture.userStatus().getId();
        UUID savedOtherUserStatusId = otherFixture.userStatus().getId();
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // 조회 대상 사용자와 비교 대상 사용자가 실제로 다른 row이고,
        // 두 UserStatus도 서로 다른 row여야 userId 필터 검증이 의미를 가진다.
        assertThat(savedUserId).isNotNull();
        assertThat(savedOtherUserId)
                .isNotNull()
                .isNotEqualTo(savedUserId);
        assertThat(savedUserStatusId).isNotNull();
        assertThat(savedOtherUserStatusId)
                .isNotNull()
                .isNotEqualTo(savedUserStatusId);

        // when
        // 조회 대상 사용자의 id로 UserStatus를 조회한다.
        // findByUserId(...)는 user_statuses.user_id 조건으로 단건을 찾고,
        // @EntityGraph(attributePaths = {"user"})로 연관된 User도 함께 조회해야 한다.
        UserStatus userStatus = userStatusRepository.findByUserId(savedUserId).orElseThrow(AssertionError::new);

        // then
        // @EntityGraph 검증은 userStatus.getUser() 또는 getUserId() 같은 getter 접근 전에 해야 한다.
        // getter를 먼저 호출하면 LAZY 연관관계가 그 시점에 초기화될 수 있어,
        // EntityGraph 때문에 미리 로딩된 것인지 구분하기 어려워진다.
        PersistenceUnitUtil persistenceUnitUtil = getPersistenceUnitUtil();
        assertThat(persistenceUnitUtil.isLoaded(userStatus, "user")).isTrue();

        // 조회된 UserStatus가 요청한 사용자의 상태 row인지 확인한다.
        // 다른 사용자에게도 상태 row가 있으므로, 이 검증은 userId 조건이 실제로 적용됐음을 보여준다.
        assertThat(userStatus.getId()).isEqualTo(savedUserStatusId);
        assertThat(userStatus.getId()).isNotEqualTo(savedOtherUserStatusId);
        assertThat(userStatus.getUserId()).isEqualTo(savedUserId);

        // lastActiveAt은 UserStatusCreateCommand에서 온 값이다.
        // H2 timestamp(6)는 마이크로초 단위까지만 저장하므로 나노초까지 완전 동일하다고 보지 않고,
        // DB 정밀도 수준의 차이만 허용한다.
        assertThat(Duration.between(userStatusCreateCommand.createdAt(), userStatus.getLastActiveAt()).abs())
                .isLessThanOrEqualTo(Duration.ofNanos(1_000));

        // User가 함께 로딩됐으므로 연관 User의 주요 필드도 저장한 command 값과 일치해야 한다.
        // 이 검증은 단순히 user_id 값만 맞는 것이 아니라 실제 User 엔티티가 올바르게 연결됐는지 확인한다.
        User foundUser = userStatus.getUser();
        assertThat(foundUser.getId()).isEqualTo(savedUserId);
        assertThat(foundUser.getUsername()).isEqualTo(userCreateCommand.username());
        assertThat(foundUser.getEmail()).isEqualTo(userCreateCommand.email());
    }

    @Test
    @DisplayName("사용자별 상태 조회 성공 - 상태가 없으면 Optional.empty 반환")
    void findByUserId_returnsEmpty_whenUserStatusDoesNotExist() {
        // given
        // 이 테스트의 대상은 UserStatusRepository.findByUserId(...) 쿼리다.
        // 여기서 검증하려는 것은 "사용자 row는 존재하지만, 그 사용자에게 연결된 UserStatus row는 없는 경우"다.
        //
        // 빈 DB에서 아무 UUID나 조회하면 Optional.empty가 나올 수밖에 없으므로 테스트 의미가 약하다.
        // 그래서 상태를 가진 사용자와 상태가 없는 사용자를 둘 다 저장한다.
        // 이렇게 하면 user_statuses 테이블에 row가 있어도, 조회 대상 user_id와 연결된 row가 없으면
        // Optional.empty를 반환해야 한다는 조건을 더 정확히 검증할 수 있다.
        UserCreateCommand userWithStatusCreateCommand = userCreateCommand();
        UserCreateCommand userWithoutStatusCreateCommand = userCreateCommand(
                "userWithoutStatus",
                "passwordWithoutStatus",
                "withoutStatus@gmail.com"
        );
        UserStatusCreateCommand userStatusCreateCommand = userStatusCreateCommand();

        // UserStatus.user는 nullable = false인 @OneToOne 연관관계이고 user_id에 unique 제약이 있다.
        // 따라서 UserStatus를 만들 대상 User를 먼저 저장하고, 그 User를 참조하는 UserStatus를 저장한다.
        UserStatusFixture fixture = saveUserWithStatus(userWithStatusCreateCommand, userStatusCreateCommand);

        // 조회 대상 사용자는 실제 users row로 저장하되 UserStatus는 만들지 않는다.
        // 이 사용자의 id로 조회했을 때 empty가 반환되어야 이 테스트가 통과한다.
        User savedUserWithoutStatus = saveUser(userWithoutStatusCreateCommand);

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있는 객체가 반환될 수 없다.
        // 즉, 아래 when 절의 결과는 1차 캐시가 아니라 실제 SELECT 결과라는 점이 분명해진다.
        UUID savedUserWithStatusId = fixture.user().getId();
        UUID savedUserWithoutStatusId = savedUserWithoutStatus.getId();
        UUID savedUserStatusId = fixture.userStatus().getId();
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // UserStatus row가 아예 없는 상황이 아니어야 하고,
        // 상태를 가진 사용자와 조회 대상 사용자는 서로 달라야 한다.
        assertThat(savedUserStatusId).isNotNull();
        assertThat(savedUserWithStatusId).isNotNull();
        assertThat(savedUserWithoutStatusId)
                .isNotNull()
                .isNotEqualTo(savedUserWithStatusId);

        // when
        // 실제로 존재하는 사용자 id지만, UserStatus가 연결되어 있지 않은 사용자 id로 조회한다.
        Optional<UserStatus> foundUserStatus = userStatusRepository.findByUserId(savedUserWithoutStatusId);

        // then
        // user_statuses 테이블에 다른 사용자의 상태 row가 있더라도,
        // 조회 대상 user_id와 연결된 상태가 없으면 Optional.empty가 반환되어야 한다.
        // 반환 엔티티가 없으므로 이 테스트에서는 EntityGraph나 User 필드 검증을 하지 않는다.
        // 연관 User 선로딩과 필드 매핑은 성공 케이스 테스트에서 별도로 검증한다.
        assertThat(foundUserStatus).isEmpty();
    }

    @Test
    @DisplayName("사용자 상태 존재 여부 조회 성공 - 사용자 상태가 있으면 true 반환")
    void existsByUser_Id_returnsTrue_whenUserStatusExists() {
        // given
        // 이 테스트의 대상은 UserStatusRepository.existsByUser_Id(...) derived query다.
        // 상태 ID와 사용자 ID 조합을 검증하는 existsByIdAndUser_Id(...)와 달리,
        // 여기서는 특정 userId에 연결된 UserStatus row가 하나라도 있는지만 확인한다.
        //
        // Repository 슬라이스 테스트이므로 Mock을 쓰지 않고 실제 User와 UserStatus 엔티티를 저장한다.
        // saveAndFlush(...)를 사용해 insert SQL을 즉시 DB에 반영하면,
        // existsByUser_Id(...)가 영속성 컨텍스트가 아니라 DB row 기준으로 동작한다는 의도가 분명해진다.
        UserCreateCommand userCreateCommand = userCreateCommand();
        UserStatusCreateCommand userStatusCreateCommand = userStatusCreateCommand();

        // UserStatus.user는 nullable = false이고 user_id에는 unique 제약이 있다.
        // 따라서 먼저 실제 User를 저장하고, 그 User를 참조하는 UserStatus를 저장한다.
        UserStatusFixture fixture = saveUserWithStatus(userCreateCommand, userStatusCreateCommand);

        UUID savedUserId = fixture.user().getId();
        UUID savedUserStatusId = fixture.userStatus().getId();
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // User와 UserStatus가 실제 DB 저장 대상이 되었고, 조회 조건으로 사용할 userId가 존재해야 한다.
        assertThat(savedUserId).isNotNull();
        assertThat(savedUserStatusId).isNotNull();

        // when
        // UserStatus가 연결된 사용자의 id로 존재 여부를 조회한다.
        boolean exists = userStatusRepository.existsByUser_Id(savedUserId);

        // then
        // user_statuses 테이블에 savedUserId를 user_id로 가진 row가 있으므로 true가 반환되어야 한다.
        // 이 테스트는 userStatusId 조건을 검증하지 않는다.
        // 상태 ID와 사용자 ID 조합 검증은 existsByIdAndUser_Id(...) 전용 테스트에서 다룬다.
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 상태 존재 여부 조회 성공 - 사용자 상태가 없으면 false 반환")
    void existsByUser_Id_returnsFalse_whenUserStatusDoesNotExist() {
        // given
        // 이 테스트의 대상도 UserStatusRepository.existsByUser_Id(...)다.
        // 검증하려는 조건은 "사용자 row는 존재하지만, 그 사용자에게 연결된 UserStatus row는 없는 경우"다.
        //
        // user_statuses 테이블이 완전히 비어 있으면 false가 나오는 것이 당연해서 테스트가 약하다.
        // 그래서 상태가 있는 사용자를 하나 저장해 user_statuses 테이블에 row를 만든 뒤,
        // 상태가 없는 다른 실제 사용자 id로 존재 여부를 조회한다.
        UserCreateCommand userWithStatusCreateCommand = userCreateCommand();
        UserCreateCommand userWithoutStatusCreateCommand = userCreateCommand(
                "userWithoutStatus",
                "passwordWithoutStatus",
                "withoutStatus@gmail.com"
        );
        UserStatusCreateCommand userStatusCreateCommand = userStatusCreateCommand();

        // 상태가 있는 사용자와 그 UserStatus를 먼저 저장한다.
        // 이 row는 existsByUser_Id(...)가 단순히 "테이블에 상태가 하나라도 있는지"가 아니라
        // 전달받은 userId 조건을 적용하는지 확인하기 위한 비교 데이터다.
        UserStatusFixture fixture = saveUserWithStatus(userWithStatusCreateCommand, userStatusCreateCommand);

        // 조회 대상 사용자는 실제 users row로 저장하되 UserStatus는 만들지 않는다.
        // 이 사용자의 id로 조회하면 false가 반환되어야 한다.
        User savedUserWithoutStatus = saveUser(userWithoutStatusCreateCommand);

        UUID savedUserWithStatusId = fixture.user().getId();
        UUID savedUserWithoutStatusId = savedUserWithoutStatus.getId();
        UUID savedUserStatusId = fixture.userStatus().getId();
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // UserStatus row가 실제로 존재하고, 상태가 있는 사용자와 조회 대상 사용자는 서로 달라야 한다.
        assertThat(savedUserStatusId).isNotNull();
        assertThat(savedUserWithStatusId).isNotNull();
        assertThat(savedUserWithoutStatusId)
                .isNotNull()
                .isNotEqualTo(savedUserWithStatusId);

        // when
        // 실제로 존재하는 사용자 id지만, UserStatus가 연결되어 있지 않은 userId로 조회한다.
        boolean exists = userStatusRepository.existsByUser_Id(savedUserWithoutStatusId);

        // then
        // user_statuses 테이블에 다른 사용자의 상태 row가 있더라도,
        // 조회 대상 userId와 연결된 row가 없으면 false가 반환되어야 한다.
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("사용자 상태 소유 여부 조회 성공 - 상태 ID와 사용자 ID가 모두 일치하면 true 반환")
    void existsByIdAndUser_Id_returnsTrue_whenIdAndUserIdMatch() {
        // given
        // 이 테스트의 대상은 UserStatusRepository.existsByIdAndUser_Id(...) derived query다.
        // existsByUser_Id(...)가 "해당 사용자에게 상태가 있는지"만 확인한다면,
        // existsByIdAndUser_Id(...)는 "이 상태 ID가 이 사용자에게 속한 상태인지"를 확인한다.
        //
        // 따라서 검증 조건은 두 가지다.
        // 1. user_statuses.id가 전달한 상태 ID와 일치해야 한다.
        // 2. user_statuses.user_id가 전달한 사용자 ID와 일치해야 한다.
        //
        // Repository 슬라이스 테스트이므로 Mock을 사용하지 않고 실제 User와 UserStatus 엔티티를 저장한다.
        // saveAndFlush(...)를 사용해 insert SQL을 즉시 DB에 반영하면,
        // existsByIdAndUser_Id(...)가 실제 DB row 기준으로 존재 여부를 판단한다는 의도가 분명해진다.
        UserCreateCommand userCreateCommand = userCreateCommand();
        UserStatusCreateCommand userStatusCreateCommand = userStatusCreateCommand();

        // UserStatus.user는 nullable = false인 @OneToOne 연관관계이고 user_id에는 unique 제약이 있다.
        // 먼저 실제 User를 저장하고, 그 User를 참조하는 UserStatus를 저장한다.
        UserStatusFixture fixture = saveUserWithStatus(userCreateCommand, userStatusCreateCommand);

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있는 객체가 반환될 수 없다.
        // 즉, 아래 when 절의 exists 쿼리는 1차 캐시가 아니라 실제 DB 조회 결과를 기준으로 판단한다.
        UUID savedUserId = fixture.user().getId();
        UUID savedUserStatusId = fixture.userStatus().getId();
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // User와 UserStatus가 실제 저장되었고, 조회 조건으로 사용할 두 식별자가 모두 존재해야 한다.
        // User ID와 UserStatus ID는 서로 다른 테이블의 식별자이므로 서로 다르다는 점을 테스트 목적에 넣지 않는다.
        assertThat(savedUserId).isNotNull();
        assertThat(savedUserStatusId).isNotNull();

        // when
        // 저장된 UserStatus의 id와 그 상태를 소유한 User의 id를 함께 넘긴다.
        boolean exists = userStatusRepository.existsByIdAndUser_Id(savedUserStatusId, savedUserId);

        // then
        // 상태 ID와 사용자 ID가 같은 row에서 동시에 일치하므로 true가 반환되어야 한다.
        // 이 테스트는 단순 사용자별 상태 존재 여부가 아니라 상태 소유권 조합을 확인한다.
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 상태 소유 여부 조회 성공 - 사용자 ID가 일치하지 않으면 false 반환")
    void existsByIdAndUser_Id_returnsFalse_whenUserIdDoesNotMatch() {
        // given
        // 이 테스트의 대상도 UserStatusRepository.existsByIdAndUser_Id(...)다.
        // 검증하려는 조건은 "상태 ID도 실제로 존재하고 사용자 ID도 실제로 존재하지만,
        // 그 사용자 ID가 해당 상태의 소유자 ID가 아닌 경우 false를 반환하는지"다.
        //
        // 임의 UUID나 상태가 없는 사용자 ID를 넘기면 false가 나오는 이유가 흐려질 수 있다.
        // 그래서 서로 다른 두 사용자를 저장하고, 두 사용자 모두 UserStatus를 갖게 만든다.
        // 그 다음 첫 번째 사용자의 상태 ID와 두 번째 사용자의 ID를 조합해 조회한다.
        UserCreateCommand ownerCreateCommand = userCreateCommand();
        UserCreateCommand otherOwnerCreateCommand = userCreateCommand(
                "otherOwner",
                "otherPassword",
                "otherOwner@gmail.com"
        );
        UserStatusCreateCommand ownerStatusCreateCommand = userStatusCreateCommand();
        UserStatusCreateCommand otherOwnerStatusCreateCommand = userStatusCreateCommand(
                ownerStatusCreateCommand.createdAt().minusSeconds(60)
        );

        // 두 UserStatus 모두 실제로 저장한다.
        // 이 구성에서는 user_statuses 테이블에도 row가 있고, users 테이블에도 조회에 사용할 user row가 있다.
        // 따라서 false 결과는 "데이터가 없어서"가 아니라 "상태 ID와 사용자 ID의 소유 관계가 맞지 않아서"여야 한다.
        UserStatusFixture ownerFixture = saveUserWithStatus(ownerCreateCommand, ownerStatusCreateCommand);
        UserStatusFixture otherOwnerFixture = saveUserWithStatus(otherOwnerCreateCommand, otherOwnerStatusCreateCommand);

        // em.clear() 이후에는 저장 직후 영속성 컨텍스트에 남아 있는 객체가 결과에 영향을 주지 않는다.
        // existsByIdAndUser_Id(...)가 실제 DB 조건으로 판단하는지 확인하기 위한 정리다.
        UUID savedOwnerId = ownerFixture.user().getId();
        UUID savedOtherOwnerId = otherOwnerFixture.user().getId();
        UUID savedOwnerStatusId = ownerFixture.userStatus().getId();
        UUID savedOtherOwnerStatusId = otherOwnerFixture.userStatus().getId();
        em.clear();

        // 사전 조건을 먼저 확인한다.
        // 두 사용자와 두 상태가 모두 실제로 저장되었고, 서로 다른 row여야 소유권 불일치 검증이 의미를 가진다.
        assertThat(savedOwnerId).isNotNull();
        assertThat(savedOtherOwnerId)
                .isNotNull()
                .isNotEqualTo(savedOwnerId);
        assertThat(savedOwnerStatusId).isNotNull();
        assertThat(savedOtherOwnerStatusId)
                .isNotNull()
                .isNotEqualTo(savedOwnerStatusId);

        // when
        // 첫 번째 사용자의 상태 ID에 두 번째 사용자의 ID를 조합한다.
        // 두 식별자는 각각 DB에 존재하지만 같은 UserStatus row에서 동시에 만족하지 않는다.
        boolean exists = userStatusRepository.existsByIdAndUser_Id(savedOwnerStatusId, savedOtherOwnerId);

        // then
        // 상태 ID만 맞거나 사용자 ID만 존재하는 것으로는 충분하지 않다.
        // 상태 ID와 사용자 ID가 같은 row의 소유 관계로 연결되어 있지 않으면 false가 반환되어야 한다.
        assertThat(exists).isFalse();
    }

    private UserCreateCommand userCreateCommand() {
        return userCreateCommand(
                "testUsername",
                "testPassword",
                "testEmail@gmail.com"
        );
    }

    private UserCreateCommand userCreateCommand(String username, String password, String email) {
        return new UserCreateCommand(username, password, email);
    }

    private UserStatusCreateCommand userStatusCreateCommand() {
        return userStatusCreateCommand(Instant.now());
    }

    private UserStatusCreateCommand userStatusCreateCommand(Instant createdAt) {
        return new UserStatusCreateCommand(createdAt);
    }

    private User saveUser(UserCreateCommand command) {
        return userRepository.saveAndFlush(new User(command, null));
    }

    private UserStatus saveUserStatus(User user, UserStatusCreateCommand command) {
        return userStatusRepository.saveAndFlush(new UserStatus(user, command));
    }

    private UserStatusFixture saveUserWithStatus(UserCreateCommand userCommand, UserStatusCreateCommand statusCommand) {
        User savedUser = saveUser(userCommand);
        UserStatus savedUserStatus = saveUserStatus(savedUser, statusCommand);

        return new UserStatusFixture(savedUser, savedUserStatus);
    }

    private PersistenceUnitUtil getPersistenceUnitUtil() {
        return em.getEntityManagerFactory().getPersistenceUnitUtil();
    }

    private record UserStatusFixture(User user, UserStatus userStatus) {
    }

}
