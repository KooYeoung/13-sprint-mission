# 2026-07-24 작업 기록 - UserRepository 테스트와 SQL 로깅

## 개요

오늘 작업은 `UserRepository`의 JPA repository 동작을 실제 DB 기반 슬라이스 테스트로 검증하고, SQL 확인과 쿼리 수 회귀 검증을 위한 로깅/통계 설정을 정리한 작업이다.

핵심 방향은 다음과 같다.

- Repository 테스트는 mock이 아니라 `@DataJpaTest`에서 실제 JPA repository, entity, 테스트 DB를 사용한다.
- 조회 테스트는 단순 반환값뿐 아니라 실제 저장된 row, 연관관계 로딩, 영속성 컨텍스트 영향을 함께 고려한다.
- `@EntityGraph(attributePaths = {"userStatus", "profile"})`가 붙은 조회 메서드는 `PersistenceUnitUtil.isLoaded(...)`로 연관 객체가 미리 로딩됐는지 확인한다.
- 사용자 삭제는 리뷰 과정에서 `deleteDirectlyById(...)` JPQL bulk delete를 검토했지만, 삭제 전 사용자 조회로 User가 이미 영속성 컨텍스트에 올라오는 흐름을 확인해 기본 `deleteById(...)` 사용으로 되돌리는 방향으로 정리했다.
- 이 결정을 위해 `MessageRepository.detachAuthorByAuthorId(...)`의 `clearAutomatically = true`를 제거해, 메시지 author detach 벌크 업데이트가 사용자 삭제 직전에 영속성 컨텍스트를 비우지 않게 했다.
- dev/test SQL 로그는 P6Spy를 기준으로 정리하고 Hibernate SQL 로그 중복 출력을 껐다.
- P6Spy 포맷터는 바인딩 값이 치환된 실행 SQL 대신 `prepared` 템플릿 SQL만 출력하도록 보안 리뷰를 반영했다.

## 관련 커밋

| 커밋 | 메시지 | 내용 |
| --- | --- | --- |
| `6466ed5` | `test: add UserRepository slice tests` | `UserRepositoryTest` 추가, 테스트 설정 추가, 테스트 SQL 로깅 설정, JPQL delete 기반 직접 삭제 검증 |
| `7814b60` | `chore: configure dev SQL logging` | dev 프로파일 P6Spy SQL 로깅 설정 추가 및 Hibernate 중복 SQL 로그 비활성화 |
| `e3a27e5` | `refactor: clarify user repository query methods` | 리뷰 중간에 User 삭제 벌크 메서드명을 `deleteDirectlyById(...)`로 명확히 변경했으나, 이후 재검토로 제거 방향으로 정리 |

## 변경 파일

### `src/test/java/com/sprint/mission/discodeit/repository/UserRepositoryTest.java`

`UserRepository`의 주요 derived query와 `@EntityGraph` 동작을 검증하는 슬라이스 테스트를 추가했다.

검증한 테스트 케이스는 다음과 같다.

- `existsByUsername_returnsTrue_whenUsernameExists`
- `existsByUsername_returnsFalse_whenUsernameDoesNotExist`
- `existsByEmail_returnsTrue_whenEmailExists`
- `existsByEmail_returnsFalse_whenEmailDoesNotExist`
- `findAll_fetchesUserStatusAndProfile_whenUsersExist`
- `findById_fetchesUserStatusAndProfile_whenUserExists`
- `findById_returnsEmpty_whenUserDoesNotExist`
- `findByUsernameAndPassword_fetchesUserStatusAndProfile_whenCredentialsMatch`
- `findByUsernameAndPassword_returnsEmpty_whenCredentialsDoNotMatch`

### `src/main/java/com/sprint/mission/discodeit/repository/UserRepository.java`

리뷰 중간에는 직접 JPQL bulk delete 메서드 `deleteDirectlyById(UUID id)`를 추가했다.

목적은 기본 `deleteById(...)`가 삭제 대상 엔티티를 먼저 조회할 수 있는지 확인하고, 선행 select를 줄이는 것이었다.

다만 이후 서비스 삭제 흐름을 다시 확인하면서 최종 방향을 조정했다.

`BasicUserService.delete(...)`는 삭제 전 `getUserRequireThrow(userId)`로 이미 User를 조회한다. 같은 트랜잭션과 영속성 컨텍스트가 유지된다면 기본 `deleteById(userId)`는 이미 로딩된 User를 대상으로 삭제할 수 있다.

문제는 `messageService.detachByAuthorId(userId)` 내부의 `MessageRepository.detachAuthorByAuthorId(...)`가 `clearAutomatically = true`를 갖고 있으면, 그 직전에 조회한 User까지 영속성 컨텍스트에서 분리할 수 있다는 점이었다.

그래서 현재 방향은 다음과 같다.

- `MessageRepository.detachAuthorByAuthorId(...)`에서 `clearAutomatically`를 제거한다.
- `UserRepository`에는 별도 벌크 삭제 메서드를 두지 않는다.
- 사용자 삭제는 Spring Data JPA 기본 `deleteById(...)`를 사용한다.
### `src/main/java/com/sprint/mission/discodeit/config/P6SpySqlFormatter.java`

P6Spy SQL 로그 포맷터를 추가했다.

역할은 다음과 같다.

- SQL 로그를 SLF4J로 출력한다.
- DDL과 일반 SQL을 Hibernate `FormatStyle`로 포맷한다.
- 빈 SQL은 출력하지 않는다.
- `statement` 카테고리만 SQL 포맷 대상으로 삼는다.
후속 보안 리뷰 반영:

- 기존 구현은 P6Spy의 `sql` 인자를 포맷했다.
- `sql` 인자는 바인딩 값이 치환된 실행 SQL일 수 있어 이메일, 비밀번호 같은 민감 값이 dev/test 로그에 노출될 수 있다.
- 현재 구현은 `prepared` 인자만 포맷한다.
- `prepared`가 비어 있으면 `sql`로 fallback하지 않고 빈 로그를 반환한다.
- 이 동작은 `P6SpySqlFormatterTest`에서 회귀 검증한다.

### `src/test/java/com/sprint/mission/discodeit/config/JpaAuditingTestConfig.java`

`@DataJpaTest`에서 `@CreatedDate`, `@LastModifiedDate`가 동작하도록 테스트용 JPA auditing 설정을 추가했다.

### `src/test/java/com/sprint/mission/discodeit/config/QuerydslTestConfig.java`

Repository 테스트 컨텍스트에서 `JPAQueryFactory` 빈을 사용할 수 있도록 테스트용 Querydsl 설정을 추가했다.

### `src/test/resources/application-test.yml`

테스트 환경에서 P6Spy 기반 SQL 로그를 활성화하고 Hibernate SQL 로그 중복 출력을 비활성화했다.

주요 변경:

- `decorator.datasource.enabled: true`
- `decorator.datasource.p6spy.enable-logging: true`
- `decorator.datasource.p6spy.logging: slf4j`
- `logging.level.p6spy: info`
- `logging.level.org.hibernate.SQL: off`
- `logging.level.org.hibernate.orm.jdbc.bind: off`

### `src/main/resources/application-dev.yml`

개발 환경에서도 P6Spy SQL 로그를 사용하도록 설정했다.

주요 변경:

- P6Spy logging 활성화
- `p6spy: info`
- Hibernate SQL 로그와 bind 로그 비활성화
- SQL 로그 중복 출력 방지

## 테스트 케이스별 작업 내용

### 1. `existsByUsername` 존재 케이스

목적:

- 실제 저장된 사용자명으로 `existsByUsername(...)`가 `true`를 반환하는지 검증한다.

보강 내용:

- `UserCreateCommand`와 `User`는 실제 객체를 사용했다.
- `saveAndFlush(...)`로 insert SQL이 즉시 DB에 반영되도록 했다.
- 저장된 id가 생성됐는지 먼저 확인했다.
- 조회 조건은 command에서 꺼낸 username을 사용해 테스트 데이터와 조회 조건의 불일치 가능성을 줄였다.

### 2. `existsByUsername` 미존재 케이스

목적:

- users 테이블에 row가 있어도 username 조건이 맞지 않으면 `false`가 반환되는지 검증한다.

보강 내용:

- 빈 테이블 조회가 아니라 다른 username을 가진 사용자를 저장한 뒤 조회했다.
- 이 방식은 "데이터가 없어서 false"가 아니라 "조건에 맞는 row가 없어서 false"임을 분명히 한다.

### 3. `existsByEmail` 존재 케이스

목적:

- 실제 저장된 email로 `existsByEmail(...)`가 `true`를 반환하는지 검증한다.

보강 내용:

- TODO를 제거하고 `given / when / then` 주석을 상세화했다.
- `existEmail`처럼 의미가 애매한 변수명 대신 `email`을 사용했다.
- 저장된 id 검증을 추가했다.

### 4. `existsByEmail` 미존재 케이스

목적:

- users 테이블에 row가 있어도 email 조건이 맞지 않으면 `false`가 반환되는지 검증한다.

보강 내용:

- 랜덤 prefix 방식 대신 고정된 `missingEmail@gmail.com`을 사용했다.
- 고정값은 실패 원인을 재현하기 쉽고 테스트 의도가 더 잘 보인다.
- 조회 email이 실제 저장된 email과 다르다는 검증을 추가했다.

### 5. `findAll` + `@EntityGraph`

목적:

- `UserRepository.findAll()`이 사용자 목록을 반환하고, `userStatus`, `profile` 연관관계를 함께 조회하는지 검증한다.

보강 내용:

- `profile`을 `null`로 두지 않고 실제 `BinaryContent`를 저장해 연결했다.
- 실제 `UserStatus`도 저장해 `userStatus` 연관관계를 검증할 수 있게 했다.
- `em.clear()`로 1차 캐시를 비운 뒤 `findAll()`을 호출했다.
- `PersistenceUnitUtil.isLoaded(foundUser, "userStatus")`와 `isLoaded(foundUser, "profile")`로 getter 호출 전에 로딩 여부를 검증했다.
- `isEqualTo(savedUser)` 같은 객체 비교 대신 id와 주요 필드 값을 직접 검증했다.

판단 이유:

- `em.clear()` 이후 조회된 엔티티는 저장 직후 엔티티와 같은 인스턴스가 아니다.
- Repository 테스트에서는 equals/hashCode에 기대기보다 어떤 row와 필드가 조회됐는지 명시적으로 드러내는 편이 좋다.

### 6. `findById` 존재 케이스

목적:

- 저장된 사용자 id로 단건 조회했을 때 User가 반환되고, `userStatus`, `profile`이 함께 로딩되는지 검증한다.

보강 내용:

- `findAll()` 설명이 복사돼 있던 주석을 `findById(...)` 책임에 맞게 수정했다.
- `savedUserId`, `savedUserStatusId`, `savedProfileId`를 `em.clear()` 전에 보관했다.
- `findById(savedUserId).orElseThrow(AssertionError::new)`로 조회 실패 시 테스트가 즉시 실패하게 했다.
- `PersistenceUnitUtil.isLoaded(...)`로 entity graph 적용 여부를 확인했다.

### 7. `findById` 미존재 케이스

목적:

- 존재하지 않는 사용자 id로 `findById(...)`를 호출하면 `Optional.empty`가 반환되는지 검증한다.
- 삭제 동작 자체를 이 테스트에 섞지 않고, 단건 조회의 negative case만 명확히 검증한다.

보강 내용:

- 테스트 DB가 완전히 비어 있어서 우연히 empty가 되는 상황을 피하기 위해 실제 User row를 하나 저장했다.
- 저장된 id와 다른 `missingUserId`를 만들어 조회 대상으로 사용했다.
- `savedUserId`가 null이 아닌지, `missingUserId`가 저장된 id와 다른지 먼저 확인했다.
- `em.clear()`로 영속성 컨텍스트를 비워 repository 조회가 실제 DB 조회 경로를 타도록 했다.

판단 이유:

- 이전에는 삭제된 id를 다시 조회하는 방식과 `deleteDirectlyById(...)` 쿼리 수 검증을 함께 넣었다.
- 하지만 사용자 삭제 최적화 방향이 기본 `deleteById(...)` 유지로 바뀌면서, 이 repository 테스트에서 삭제 쿼리 수를 고정할 이유가 없어졌다.
- `findById_returnsEmpty_whenUserDoesNotExist`는 이제 이름 그대로 존재하지 않는 id 조회 결과만 검증한다.
### 8. `findByUsernameAndPassword` 인증 성공 케이스

목적:

- username과 password가 모두 일치하면 User가 반환되는지 검증한다.
- 인증 조회 결과에 `userStatus`, `profile`이 함께 로딩되는지 검증한다.

보강 내용:

- 실제 `BinaryContent`와 `UserStatus`를 저장해 연관관계 조회를 검증했다.
- `em.clear()` 후 실제 DB에서 다시 조회하도록 했다.
- `PersistenceUnitUtil.isLoaded(...)`로 entity graph 적용 여부를 확인했다.
- `id`, `username`, `password`, `email`을 검증해 잘못된 row 반환 가능성을 줄였다.
- `profile`, `UserStatus`는 id 기준으로 실제 저장한 연관 엔티티인지 확인했다.

### 9. `findByUsernameAndPassword` 인증 실패 케이스

목적:

- username 또는 password 중 하나라도 일치하지 않으면 `Optional.empty`가 반환되는지 검증한다.

보강 내용:

- positive 케이스에서 복사된 `@EntityGraph`/profile/UserStatus 관련 주석을 제거했다.
- 실패 인증 조회에서는 반환 엔티티가 없으므로 연관관계 로딩을 검증할 수 없다.
- 실제 User row만 저장해서 "빈 테이블이라 empty"가 아닌 상황을 만들었다.
- username 불일치 케이스와 password 불일치 케이스를 각각 검증했다.

검증 조건:

- `wrongUsername + correctPassword` -> empty
- `correctUsername + wrongPassword` -> empty

## 주요 설계 판단

### 실제 객체와 mock 선택

이번 작업은 repository slice test이므로 mock을 사용하지 않았다.

- Repository: 실제 Spring Data JPA repository
- Entity: 실제 `User`, `UserStatus`, `BinaryContent`
- DTO/command: 실제 `UserCreateCommand`, `UserStatusCreateCommand`
- DB: H2 in-memory test DB

이는 `AGENTS.md`의 repository 테스트 기준과 맞는다.

### `saveAndFlush(...)` 사용

대부분의 테스트에서 `saveAndFlush(...)`를 사용했다.

이유:

- 저장이 영속성 컨텍스트에만 머문 상태와 DB에 flush된 상태를 구분하기 위함이다.
- Repository derived query가 실제 DB row 기준으로 동작하는지 테스트 의도를 명확히 한다.

### `em.clear()` 사용

조회 테스트에서는 저장 후 `em.clear()`를 사용했다.

이유:

- 1차 캐시에 남아 있는 엔티티가 그대로 반환되면 실제 query/fetch 동작을 검증했다고 보기 어렵다.
- `@EntityGraph` 적용 여부를 확인하려면 DB에서 다시 조회되는 상황을 만들어야 한다.

### `PersistenceUnitUtil.isLoaded(...)` 사용

`@EntityGraph` 검증에서는 getter 접근 전에 `isLoaded(...)`를 먼저 호출했다.

이유:

- `foundUser.getUserStatus()` 같은 getter를 먼저 호출하면 lazy loading이 발생할 수 있다.
- 그러면 entity graph로 미리 조회된 것인지, getter 접근으로 뒤늦게 조회된 것인지 구분하기 어렵다.

### entity equals/hashCode 관련 판단

작업 중 엔티티 equality에 대한 검토가 있었다.

최종 테스트에서는 `assertThat(foundUser).isEqualTo(savedUser)`처럼 엔티티 객체 자체를 비교하지 않고, id와 주요 필드 값을 직접 비교했다.

이유:

- JPA 엔티티의 equals/hashCode 구현 방식은 프록시, 영속화 전 id, 영속성 컨텍스트 상태에 영향을 받기 쉽다.
- Repository 테스트의 목적은 "어떤 row와 연관 데이터가 조회됐는지"를 확인하는 것이므로 id/필드 비교가 더 명확하다.

## 검증 명령

작업 중 반복적으로 사용한 주요 검증 명령은 다음과 같다.

```powershell
.\gradlew.bat test --tests "com.sprint.mission.discodeit.repository.UserRepositoryTest"
```

개별 케이스 검증에 사용한 명령:

```powershell
.\gradlew.bat test --tests "com.sprint.mission.discodeit.repository.UserRepositoryTest.findById_returnsEmpty_whenUserDoesNotExist"
.\gradlew.bat test --tests "com.sprint.mission.discodeit.repository.UserRepositoryTest.findByUsernameAndPassword_fetchesUserStatusAndProfile_whenCredentialsMatch"
.\gradlew.bat test --tests "com.sprint.mission.discodeit.repository.UserRepositoryTest.findByUsernameAndPassword_returnsEmpty_whenCredentialsDoNotMatch"
```

최종 확인 결과:

- `UserRepositoryTest` 전체 `BUILD SUCCESSFUL`
- dev SQL logging 설정 커밋은 별도 테스트 실행 없이 설정 diff 확인 후 커밋

## 참고할 만한 위험과 후속 점검 후보

### 1. 사용자 삭제와 영속성 컨텍스트

사용자 삭제 흐름에서는 먼저 `getUserRequireThrow(userId)`로 User를 조회한다.

따라서 같은 트랜잭션에서 영속성 컨텍스트가 유지된다면, 이후 `userRepository.deleteById(userId)`가 다시 select를 실행할 필요가 줄어든다.

이번 검토에서 중요한 변수는 UserRepository가 아니라 메시지 author detach 벌크 업데이트였다.

`MessageRepository.detachAuthorByAuthorId(...)`가 `clearAutomatically = true`를 사용하면 벌크 업데이트 후 영속성 컨텍스트가 비워지고, 앞에서 조회해 둔 User도 분리될 수 있다. 이 경우 기본 `deleteById(...)`가 삭제 시 다시 조회할 가능성이 생긴다.

현재 정리한 방향:

- 메시지 author detach는 메시지 엔티티 상태를 들고 사용하는 흐름이 아니므로 `clearAutomatically`를 제거한다.
- 사용자 삭제는 JPA 기본 삭제 흐름인 `deleteById(...)`를 사용한다.
- JPQL bulk delete는 영속성 컨텍스트, entity lifecycle callback, cascade 관점에서 우회 성격이 있으므로 지금 요구사항에는 과하다.
### 2. 쿼리 수 검증은 제한적으로 유지

`Statistics.getPrepareStatementCount()` 검증은 회귀 방지에 유용하지만, 모든 repository 테스트에 넣으면 구현 세부사항에 과하게 묶일 수 있다.

유지 기준:

- 성능 회귀 방지가 테스트 목적일 때
- N+1 방지나 불필요한 select 제거처럼 쿼리 수 자체가 요구사항일 때

이번 후속 정리에서는 `findById_returnsEmpty_whenUserDoesNotExist`에서 쿼리 수 검증을 제거했다. 해당 테스트의 책임은 존재하지 않는 id 조회 결과 검증이고, 삭제 쿼리 최적화는 현재 최종 설계의 핵심 요구사항이 아니기 때문이다.

### 3. P6Spy와 Hibernate SQL 로그 중복

dev/test에서 P6Spy를 켜면서 Hibernate SQL 로그는 껐다.

이유:

- 같은 SQL이 P6Spy와 Hibernate 양쪽에서 중복 출력되면 테스트 로그가 읽기 어려워진다.
- P6Spy 쪽으로 SQL 포맷과 출력 채널을 통일하는 편이 디버깅에 좋다.

### 4. 실패 인증 테스트에서 entity graph는 검증하지 않음

`findByUsernameAndPassword_returnsEmpty_whenCredentialsDoNotMatch`는 반환값이 empty이므로 `userStatus`, `profile` 로딩 여부를 검증할 대상이 없다.

따라서 연관관계 setup을 제거하고 username/password 조건 적용만 검증했다.

이 판단은 나중에 비슷한 negative repository 테스트를 작성할 때 참고할 만하다.

## 현재 작업트리 참고 사항

문서 갱신 시점 기준으로 사용자 삭제 흐름 정리와 P6Spy 보안 리뷰 반영은 각각 별도 커밋으로 분리했다.

작업 범위에서 제외한 untracked 항목:

- `request.md`
- `src/test/java/com/sprint/mission/discodeit/repository/BinaryContentRepositoryTest.java`
- `src/test/java/com/sprint/mission/discodeit/repository/ChannelRepositoryTest.java`
- `src/test/java/com/sprint/mission/discodeit/repository/MessageFileRepositoryTest.java`
- `src/test/java/com/sprint/mission/discodeit/repository/MessageRepositoryTest.java`
- `src/test/java/com/sprint/mission/discodeit/repository/ReadStatusRepositoryTest.java`
- `src/test/java/com/sprint/mission/discodeit/repository/UserStatusRepositoryTest.java`

## 최종 커밋 상태

문서 작성 기준으로 관련 커밋은 다음과 같다.

```text
501cf7e fix: avoid logging bound SQL values
a496964 refactor: use default user delete flow
e3a27e5 refactor: clarify user repository query methods
7814b60 chore: configure dev SQL logging
6466ed5 test: add UserRepository slice tests
```

`e3a27e5`의 `deleteDirectlyById(...)` 방향은 이후 삭제 흐름 재검토로 조정되었고, 최종 코드는 `a496964`에서 기본 `deleteById(...)` 흐름으로 정리했다.