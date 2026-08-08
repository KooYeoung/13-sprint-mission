# 사용자 삭제 시 UserStatus 명시 삭제 결정 Evidence

## 목적

사용자 삭제 API 통합 테스트를 추가하는 과정에서 `DELETE /api/users/{userId}` 호출이 500 응답을 반환하는 문제를 확인했다.

실패 원인은 단순히 `users` 삭제 SQL과 `user_statuses` 삭제 SQL의 실행 순서가 뒤바뀐 문제가 아니었다. 실제 로그에서는 `userRepository.deleteById(...)`까지 도달하기 전에, `readStatusService.deleteByUserId(...)` 실행 과정에서 flush가 발생하며 Hibernate 예외가 발생했다.

```text
BasicUserService.delete 시작
UserStatusService.delete 완료
ReadStatusService.deleteByUserId 시작
flush 발생
TransientObjectException 발생
```

핵심 문제는 `User` 엔티티가 영속성 컨텍스트 안에서 `UserStatus`를 참조하고 있는데, 서비스가 `UserStatus`만 먼저 직접 삭제했다는 점이다.

---

## 문제 발생 당시 연관관계

문제 발생 당시 `User`와 `UserStatus`는 양방향 `OneToOne` 관계였고, `User` 쪽에 삭제 cascade가 설정되어 있었다.

[User.java](../src/main/java/com/sprint/mission/discodeit/entity/User.java)

```java
@OneToOne(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.REMOVE)
private UserStatus userStatus;
```

[UserStatus.java](../src/main/java/com/sprint/mission/discodeit/entity/UserStatus.java)

```java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false, unique = true)
private User user;
```

이 매핑에서는 `User`가 삭제될 때 `UserStatus`도 함께 삭제되도록 `cascade = CascadeType.REMOVE`가 설정되어 있다.

따라서 삭제 책임은 이미 `User` 엔티티 삭제에 포함되어 있다.

---

## 문제 발생 흐름

기존 서비스 코드는 다음과 같이 `UserStatus`를 직접 먼저 삭제했다.

```java
User user = getUserRequireThrow(userId);

userStatusService.delete(user.getStatusId(), userId);
readStatusService.deleteByUserId(userId);
messageService.detachByAuthorId(userId);

userRepository.deleteById(user.getId());
```

하지만 이 흐름에서는 영속성 컨텍스트에 관리 중인 `User`가 여전히 `userStatus` 필드를 통해 삭제 대상 `UserStatus`를 참조할 수 있다.

그 상태에서 `readStatusService.deleteByUserId(...)`의 repository 호출이 실행되면, Hibernate는 쿼리 실행 전에 flush를 수행한다. 이때 관리 중인 객체 그래프 안에 이미 삭제된 `UserStatus` 참조가 남아 있어 다음 예외가 발생했다.

```text
InvalidDataAccessApiUsageException
Caused by: TransientObjectException
persistent instance references an unsaved transient instance of 'com.sprint.mission.discodeit.entity.UserStatus'
```

즉 문제의 본질은 SQL 삭제 순서 자체가 아니라, `cascade = REMOVE`가 설정된 연관 엔티티를 서비스가 별도로 먼저 삭제하면서 영속성 컨텍스트의 객체 그래프가 불일치해진 것이다.

---

## 의문점: 왜 이 시점에 flush가 발생했는가?

`BasicUserService.delete(...)` 안에서는 아직 `userRepository.deleteById(...)`를 호출하지 않았는데, 왜 `readStatusService.deleteByUserId(...)` 단계에서 flush가 발생했는지 의문이 생길 수 있다.

이유는 Hibernate의 기본 flush 모드가 `AUTO`이기 때문이다. JPA는 JPQL, Querydsl, Spring Data JPA derived query 같은 repository 쿼리를 실행하기 전에, 현재 영속성 컨텍스트의 변경 사항을 데이터베이스에 먼저 반영해 쿼리 결과와 메모리 상태가 어긋나지 않도록 flush할 수 있다.

현재 `ReadStatusService.deleteByUserId(...)`는 다음 순서로 repository를 호출한다.

```java
public void deleteByUserId(UUID userId) {
    if (!readStatusRepository.existsByUser_Id(userId)) return;

    readStatusRepository.deleteByUser_Id(userId);
}
```

즉 `existsByUser_Id(...)` 조회 쿼리 또는 `deleteByUser_Id(...)` 삭제 쿼리를 실행하기 전에 Hibernate가 앞서 발생한 `UserStatus` 삭제 변경을 flush하려고 할 수 있다.

이때 영속성 컨텍스트 안의 `User`는 아직 삭제되지 않았고, `userStatus` 필드로 방금 삭제 처리된 `UserStatus`를 계속 참조할 수 있다. 그래서 실제 사용자 삭제 SQL까지 도달하기 전에도 다음 문제가 먼저 드러난다.

```text
관리 중인 User
    -> 삭제 처리된 UserStatus 참조

다음 repository 쿼리 실행
    -> Hibernate AUTO flush
    -> 객체 그래프 불일치 감지
    -> TransientObjectException
```

따라서 예외가 `readStatusService.deleteByUserId(...)`에서 보였더라도, 원인은 `ReadStatus` 삭제 로직 자체가 아니라 그 이전에 `UserStatus`를 직접 삭제해 놓은 상태에 있다.

---

## 선택지

### 1. cascade 유지, 직접 삭제 제거

`UserStatus`를 `User`의 생명주기에 종속된 데이터로 보고, `User` 삭제 시 cascade가 함께 제거하도록 둔다.

```java
readStatusService.deleteByUserId(userId);
messageService.detachByAuthorId(userId);

userRepository.deleteById(user.getId());
```

장점:

* 현재 엔티티 매핑 의도와 일치한다.
* `UserStatus` 삭제 책임이 한 곳으로 모인다.
* 영속성 컨텍스트에서 `User`가 삭제 대상 `UserStatus`를 참조하는 중간 상태를 만들지 않는다.
* 서비스 삭제 흐름이 단순해진다.

단점:

* 삭제 SQL의 세부 실행은 JPA cascade 동작에 맡긴다.
* `UserStatus`가 독립 생명주기를 가져야 하는 요구가 생기면 매핑을 다시 봐야 한다.

### 2. cascade 제거, 서비스에서 직접 삭제

`UserStatus`를 서비스가 명시적으로 삭제하도록 하고, `User` 엔티티의 `cascade = CascadeType.REMOVE`를 제거한다.

```java
UUID userStatusId = user.getStatusId();
user.detachUserStatus();

readStatusService.deleteByUserId(userId);
messageService.detachByAuthorId(userId);
if (userStatusId != null) {
    userStatusService.delete(userStatusId, userId);
}

userRepository.deleteById(userId);
```

이 방식을 선택한다면 `User`가 이후 명시 삭제될 `UserStatus`를 계속 참조하지 않도록 연관관계를 먼저 끊어야 한다.

```java
public void detachUserStatus() {
    this.userStatus = null;
}
```

장점:

* 서비스 코드에서 삭제 순서가 명시적으로 드러난다.
* cascade에 의존하지 않는다.
* 삭제 요구사항이 바뀌었을 때 서비스 흐름에서 조정하기 쉽다.
* 연관 데이터 삭제 정책을 JPA 매핑보다 애플리케이션 유스케이스에 가깝게 둘 수 있다.

단점:

* 현재 매핑 변경이 필요하다.
* 양방향 연관관계 정리 책임이 추가된다.
* 서비스가 더 많은 영속성 세부사항을 알게 된다.

---

## 최종 결정

최종적으로 `cascade = CascadeType.REMOVE`를 제거하고, `BasicUserService.delete(...)`에서 `UserStatus`를 명시 삭제하는 방식으로 결정했다.

이유는 JPA의 자동 삭제 기능에 삭제 정책을 숨기기보다, 사용자 삭제 유스케이스에서 어떤 연관 데이터를 어떤 순서로 정리하는지 명시적으로 드러내는 편이 현재 요구사항에 더 적합하다고 판단했기 때문이다.

특히 이후 요구사항이 바뀌어 `UserStatus`를 보존하거나, 삭제 전에 감사 로그를 남기거나, 삭제 조건을 분기해야 하는 경우 서비스 흐름에서 변경 지점을 찾고 조정하기 쉽다.

다만 명시 삭제 방식을 선택하더라도 이전 실패 원인은 피해야 한다. 따라서 `UserStatus`를 삭제하기 전에 `User`가 들고 있는 역방향 참조를 먼저 끊고, 다른 repository 쿼리가 먼저 실행되더라도 삭제 예정인 `UserStatus` 참조가 flush 과정에서 문제를 일으키지 않도록 한다.

최종 삭제 흐름은 다음과 같다.

```text
1. User 조회
2. UserStatus id 확보
3. User.userStatus 역방향 참조 해제
4. ReadStatus 삭제
5. Message.author 연결 해제
6. UserStatus 명시 삭제
7. User 삭제
8. Profile BinaryContent가 있으면 파일/메타데이터 삭제
```

핵심 결론은 다음과 같다.

```text
UserStatus 삭제는 cascade와 직접 삭제 중 하나만 책임져야 한다.
최종 선택은 cascade 제거 + 서비스 명시 삭제다.
단, 직접 삭제 전에 User.userStatus 역방향 참조를 끊어
영속성 컨텍스트의 객체 그래프 불일치를 피한다.
```

---

## 변경 사항

[User.java](../src/main/java/com/sprint/mission/discodeit/entity/User.java)

```java
@OneToOne(fetch = FetchType.LAZY, mappedBy = "user")
private UserStatus userStatus;

public void detachUserStatus() {
    this.userStatus = null;
}
```

[BasicUserService.java](../src/main/java/com/sprint/mission/discodeit/service/basic/BasicUserService.java)

```java
public void delete(UUID userId) {
    User user = getUserRequireThrow(userId);
    UUID userStatusId = user.getStatusId();
    user.detachUserStatus();

    readStatusService.deleteByUserId(userId);
    messageService.detachByAuthorId(userId);
    if (userStatusId != null) {
        userStatusService.delete(userStatusId, userId);
    }

    userRepository.deleteById(user.getId());

    if (user.isProfileImageExist()) {
        binaryContentService.delete(user.getProfile());
    }
}
```

[UserServiceTest.java](../src/test/java/com/sprint/mission/discodeit/service/UserServiceTest.java)

* 사용자 삭제 성공 테스트에서 `userStatusService.delete(...)` 직접 호출을 검증한다.
* `User.userStatus` 역방향 참조가 삭제 흐름에서 해제되는지 검증한다.

[DiscodeitApiIntegrationTest.java](../src/test/java/com/sprint/mission/discodeit/integration/DiscodeitApiIntegrationTest.java)

* `DELETE /api/users/{userId}` 호출이 204를 반환하는지 검증한다.
* 삭제 후 `users`와 `user_statuses` row가 실제 DB에서 제거되는지 검증한다.

---

## 검증 결과

대상 테스트:

```shell
.\gradlew.bat --no-daemon test --tests "com.sprint.mission.discodeit.service.UserServiceTest" --tests "com.sprint.mission.discodeit.integration.DiscodeitApiIntegrationTest"
```

결과:

```text
BUILD SUCCESSFUL
```

전체 테스트:

```shell
.\gradlew.bat --no-daemon test
```

결과:

```text
BUILD SUCCESSFUL
```
