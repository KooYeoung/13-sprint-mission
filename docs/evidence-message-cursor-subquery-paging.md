# UUID 단독 커서 검토와 createdAt + UUID 복합 페이징 구현 Evidence

## 목적

메시지 목록을 최신순으로 조회하기 위해 처음에는 PK인 UUID를 단독 커서로 사용하는 방식을 고려했다.

UUID는 고유하며 PK 인덱스를 활용할 수 있으므로 커서 페이징에 적합하다고 판단했다. 하지만 UUID 타입 자체가 생성 순서를 보장하는 것은 아니며, 현재 프로젝트에서 사용하는 UUID 생성 방식만으로는 UUID 정렬 순서와 메시지 생성 순서가 일치한다고 볼 수 없다.

또한 UUID 버전은 데이터베이스 타입으로 고정되는 것이 아니라 데이터베이스 함수, 확장 모듈, ORM 또는 애플리케이션 생성기에 따라 달라질 수 있다. 따라서 특정 UUID 버전의 시간 정렬 특성에 의존하면 UUID 생성 방식에 조회 정책이 결합될 수 있다.

메시지 생성 시각인 `createdAt`을 커서로 사용하면 최신순 조회는 가능하지만, 동일한 `createdAt`을 가진 메시지가 존재할 경우 `createdAt` 단독 조건으로는 데이터가 누락될 수 있다.

따라서 `createdAt`을 주 정렬 기준으로 사용하고, 동일한 시간의 메시지 사이에서는 UUID를 보조 정렬 기준으로 사용하는 `createdAt + UUID` 복합 기준을 선택했다.

핵심 결론은 다음과 같다.

```text
UUID를 단독 커서로 사용할 수 없는 것이 아니다.
UUID 단독으로는 현재 요구사항인 메시지 생성 시간순을 안정적으로 표현할 수 없으므로
createdAt + UUID 복합 기준을 선택했다.
```

---

## 현재 UUID 생성 관련 확인

현재 엔티티 공통 식별자는 [BaseEntity.java](../src/main/java/com/sprint/mission/discodeit/entity/base/BaseEntity.java)에 정의되어 있다.

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

애플리케이션의 일반 JPA 저장 경로에서는 Hibernate가 UUID를 생성할 수 있다.

반면 Flyway V1 DDL에는 각 테이블의 PK 기본값으로 `gen_random_uuid()`가 선언되어 있다.

```sql
id uuid PRIMARY KEY DEFAULT gen_random_uuid()
```

[V1__init_schema.sql](../src/main/resources/db/migration/V1__init_schema.sql) 상단 주석에도 `gen_random_uuid()`를 랜덤 UUID 생성 함수로 설명하고 있다.

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

정리하면 현재 프로젝트에는 두 가지 근거가 함께 존재한다.

```text
JPA Entity
    @GeneratedValue(strategy = GenerationType.UUID)

PostgreSQL DDL
    DEFAULT gen_random_uuid()
```

따라서 evidence의 핵심 근거를 “DB마다 사용하는 UUID 버전이 달라 UUID 단독 커서를 사용할 수 없다”로 두면 부정확하다.

더 정확한 근거는 다음과 같다.

```text
UUID 타입 자체는 생성 시간순 정렬을 보장하지 않는다.
현재 저장 경로가 Hibernate 생성이든 DB의 gen_random_uuid() 기본값이든,
UUID 정렬 순서가 메시지 createdAt 최신순과 일치한다는 계약은 없다.
따라서 메시지 최신 생성 시간순 조회를 UUID 단독 기준에 의존하지 않는다.
```

---

## 1. UUID 단독 커서 검토

UUID만으로 커서 페이징을 할 수는 있다.

```sql
WHERE id < :cursorId
ORDER BY id DESC
```

이 방식은 다음 특성을 가진다.

```text
장점
    PK를 그대로 cursor로 사용한다.
    값이 유일하다.
    UUID 기준으로는 안정적인 순서를 만들 수 있다.

한계
    UUID 정렬 순서가 메시지 생성 시간순이라는 보장이 없다.
    최신 메시지 조회 요구사항을 직접 표현하지 못한다.
```

특히 PostgreSQL `pgcrypto`의 `gen_random_uuid()`는 랜덤 UUID를 생성한다. UUID v4처럼 무작위성이 있는 UUID는 생성 순서와 정렬 순서가 일치하지 않는다.

다만 핵심은 특정 함수 하나가 아니라, UUID 생성 방식 전체에 시간순 조회 정책을 의존하지 않는다는 점이다.

```text
UUID 버전은 DB 타입으로 고정되지 않는다.
DB 함수, 확장 모듈, ORM, 애플리케이션 생성기에 따라 달라질 수 있다.
따라서 UUID 단독 정렬에 메시지 최신순 의미를 부여하면 생성 방식에 과하게 결합된다.
```

결론적으로 UUID 단독 커서는 가능하지만, 현재 요구사항인 “메시지 최신 생성 시간순 조회”의 기준으로는 적합하지 않다.

---

## 2. createdAt 단독 커서 검토

최신순 조회만 보면 `createdAt`을 사용하는 것이 자연스럽다.

```sql
WHERE created_at < :cursorCreatedAt
ORDER BY created_at DESC
```

하지만 동일한 `createdAt`을 가진 메시지가 여러 개 존재하면 일부 메시지가 누락될 수 있다.

```text
메시지 A: 10:00:00
메시지 B: 10:00:00
메시지 C: 10:00:00
```

페이지 마지막이 B인데 다음 페이지 조건을 `< 10:00:00`으로 조회하면 같은 시각의 C는 조회되지 않는다.

즉, `createdAt`은 메시지 최신순이라는 주 정렬 기준으로 적절하지만 단독 커서로는 완전한 경계를 만들기 어렵다.

---

## 3. 최종 정렬 기준 결정

메시지의 논리적인 정렬 및 커서 기준은 다음과 같이 결정했다.

```sql
ORDER BY created_at DESC, id DESC
```

다음 페이지 조건은 다음과 같다.

```sql
WHERE channel_id = :channelId
  AND (
      created_at < :cursorCreatedAt
      OR (
          created_at = :cursorCreatedAt
          AND id < :cursorId
      )
  )
ORDER BY created_at DESC, id DESC
```

여기서 UUID는 생성 순서를 표현하지 않는다. 같은 `createdAt` 안에서 고유하고 일관된 순서를 부여하는 동률 해소 기준으로만 사용한다.

따라서 외부 API 커서는 UUID 단일 값이지만, 내부 페이징 기준은 `createdAt + UUID` 복합 기준이다.

---

## 4. 복합 기준을 해석하는 위치 검토

정렬 기준 결정과 별개로, UUID cursor를 `createdAt + UUID` 복합 기준으로 해석하는 위치는 여러 선택지가 있다.

### 선택지 A: Service Projection 조회

Service가 UUID cursor를 파싱한 뒤 Repository에서 커서 메시지의 위치만 Projection으로 조회한다.

```sql
SELECT created_at, id
FROM messages
WHERE id = :cursorId
  AND channel_id = :channelId
```

그다음 Repository에 `createdAt + id` 값을 넘겨 실제 목록을 조회한다.

장점:

* Service에서 cursor 해석 흐름이 명확하다.
* 잘못된 cursor와 빈 결과를 구분하기 쉽다.
* 다른 채널 메시지 UUID를 cursor로 사용하는 경우를 명확한 예외로 처리할 수 있다.
* Repository의 목록 조회 Querydsl 조건이 단순하다.

단점:

* cursor가 있을 때 쿼리가 1회 추가된다.
* cursor 메시지가 삭제되면 위치를 복원할 수 없다.

### 선택지 B: Repository 서브쿼리

Repository가 UUID cursor를 받아 서브쿼리로 cursor 메시지의 `createdAt`을 찾는다.

장점:

* Repository 호출 한 번으로 목록 조회가 끝난다.
* Service가 페이징 세부 조건을 몰라도 된다.
* 외부 API cursor를 UUID로 유지하면서 내부에서는 복합 기준을 적용할 수 있다.

단점:

* Querydsl 코드가 복잡해진다.
* 같은 서브쿼리가 조건 안에서 반복될 수 있다.
* cursor가 존재하지 않을 때 “잘못된 cursor”와 “조회 결과 없음”을 명확히 구분하기 어렵다.

### 선택지 C: 자체 완결형 문자열 커서

`createdAt`과 `id`를 JSON으로 만든 뒤 Base64 URL로 인코딩한다.

```json
{
  "createdAt": "2026-07-28T09:30:15.123456Z",
  "id": "7d5bdbb6-4dae-4d28-8bb2-305b1258b274"
}
```

사용한다면 버전을 포함하는 형태가 안전하다.

```text
v1.eyJjcmVhdGVkQXQiOi...
```

장점:

* cursor 메시지 조회가 필요 없다.
* cursor 메시지가 삭제돼도 동작한다.
* 목록 조회 쿼리 한 번으로 처리할 수 있다.

단점:

* cursor 명세가 복잡해진다.
* 인코딩과 디코딩 구현이 필요하다.
* 버전 관리가 필요하다.

현재 프로젝트에서는 cursor 명세 복잡도를 늘리지 않기 위해 선택하지 않았다.

---

## 5. 이번 구현의 최종 결정

이번 구현은 Repository 서브쿼리 방식을 선택했다.

```text
API cursor
    UUID 문자열 유지

Service
    UUID cursor를 Repository에 그대로 전달
    실제 응답한 마지막 Message id를 nextCursor로 생성

Repository
    cursor UUID가 있으면 같은 channelId의 cursor 메시지 createdAt을 서브쿼리로 조회
    createdAt DESC, id DESC 정렬
    createdAt + id 복합 조건 적용
    pageSize + 1 조회

Response
    실제 응답한 마지막 메시지 UUID 문자열을 nextCursor로 반환
```

최종 SQL 의미는 다음과 같다.

```sql
WHERE message.channel_id = :channelId
  AND (
      message.created_at < (
          SELECT cursor_message.created_at
          FROM messages cursor_message
          WHERE cursor_message.id = :cursorId
            AND cursor_message.channel_id = :channelId
      )
      OR (
          message.created_at = (
              SELECT cursor_message.created_at
              FROM messages cursor_message
              WHERE cursor_message.id = :cursorId
                AND cursor_message.channel_id = :channelId
          )
          AND message.id < :cursorId
      )
  )
ORDER BY message.created_at DESC, message.id DESC
LIMIT :pageSizePlusOne
```

외부에 노출되는 cursor는 UUID 단일 값이지만, 실제 데이터베이스 페이징 경계는 `createdAt + UUID` 복합 기준이다.

---

## 6. 실제 코드 변경

### Controller

[MessageController.java](../src/main/java/com/sprint/mission/discodeit/controller/MessageController.java)는 `cursor` query parameter를 UUID로 받는다.

```java
@RequestParam(required = false) UUID cursor
```

### Service

[BasicMessageService.java](../src/main/java/com/sprint/mission/discodeit/service/basic/BasicMessageService.java)는 cursor를 Repository에 그대로 전달한다.

```java
Slice<Message> messageSlice = getMessageSliceDsl(channelId, pageable, cursor);
```

`nextCursor`는 추가로 조회한 메시지가 아니라 실제 응답한 마지막 메시지의 ID다.

```java
Object nextCursor = null;
if (messageSlice.hasNext() && !content.isEmpty()) {
    nextCursor = content.get(content.size() - 1).getId().toString();
}
```

### Repository

[MessageRepositoryCustomImpl.java](../src/main/java/com/sprint/mission/discodeit/repository/impl/MessageRepositoryCustomImpl.java)는 cursor가 없으면 첫 페이지를 조회한다.

```java
if (cursor == null) return null;
```

cursor가 있으면 같은 채널에 속한 cursor 메시지의 `createdAt`을 서브쿼리로 찾는다.

```java
QMessage cursorMessage = new QMessage("cursorMessage");
var cursorCreatedAt = JPAExpressions
        .select(cursorMessage.createdAt)
        .from(cursorMessage)
        .where(
                cursorMessage.id.eq(cursor),
                cursorMessage.channel.id.eq(channelId)
        );
```

그다음 `createdAt + id` 복합 조건을 적용한다.

```java
return message.createdAt.lt(cursorCreatedAt)
        .or(
                message.createdAt.eq(cursorCreatedAt)
                        .and(message.id.lt(cursor))
        );
```

정렬은 커서 기준과 같은 순서로 고정한다.

```java
.orderBy(message.createdAt.desc(), message.id.desc())
.limit(pageSize + 1L)
```

서브쿼리에는 `cursorMessage.channel.id.eq(channelId)` 조건을 포함한다. 그래야 다른 채널의 메시지 UUID가 현재 채널의 cursor 위치로 해석되지 않는다.

---

## 7. 변경 전후 Repository 조건 비교

변경 전에는 `Instant cursor`를 받아 `createdAt` 단독 조건을 적용했다.

```java
return message.createdAt.lt(cursor);
```

이 방식은 같은 `createdAt`을 가진 메시지가 여러 개 있을 때 다음 페이지에서 누락될 수 있다.

변경 후에는 `UUID cursor`를 받아 cursor 메시지의 `createdAt`을 서브쿼리로 찾고, `createdAt + id` 복합 조건을 적용한다.

```java
return message.createdAt.lt(cursorCreatedAt)
        .or(
                message.createdAt.eq(cursorCreatedAt)
                        .and(message.id.lt(cursor))
        );
```

---

## 8. size + 1 조회와 nextCursor

다음 데이터 존재 여부는 `pageSize + 1`개 조회로 판단한다.

```java
.limit(pageSize + 1L)
```

조회 결과가 `pageSize`보다 많으면 `hasNext=true`로 판단하고, 응답 content에서는 초과 조회한 1건을 제거한다.

```java
boolean hasNext = messages.size() > pageSize;

if (hasNext) {
    messages.remove(pageSize);
}
```

`nextCursor`는 초과 조회한 메시지가 아니라 실제 응답 content의 마지막 메시지 ID다.

```java
nextCursor = content.get(content.size() - 1).getId().toString();
```

---

## 9. 인덱스 판단

채널별 최신 메시지 조회와 복합 커서 조건을 고려하면 다음 인덱스는 후보가 될 수 있다.

```sql
CREATE INDEX idx_message_channel_created_at_id
ON messages (
    channel_id,
    created_at DESC,
    id DESC
);
```

PK 인덱스가 있어도 `channel_id` 필터와 `created_at DESC, id DESC` 정렬을 함께 만족시키기 어렵기 때문이다.

다만 이번 작업에서는 명확한 성능 측정 없이 DB 마이그레이션을 추가하지 않았다. 인덱스 추가는 실제 실행 계획과 성능 측정 결과를 근거로 별도 변경에서 판단한다.

---

## 10. UUID v7 검토

PostgreSQL 18부터는 `uuidv7()` 내장 함수를 사용할 수 있다.

```sql
uuidv7()
```

UUID v7은 생성 시각을 포함하므로 UUID v4보다 시간순 정렬에 유리하다. 하지만 여러 서버의 시계 차이나 트랜잭션 커밋 순서까지 포함한 엄밀한 저장 순서를 보장하지는 않는다.

엄밀한 순번이 필요하다면 UUID 버전에 의존하기보다 데이터베이스 시퀀스와 같은 별도의 단조 증가 값을 사용하는 것이 더 적절하다.

현재 요구사항은 엄밀한 DB 커밋 순서가 아니라 메시지 생성 시간순 최신 조회다. 따라서 `createdAt DESC, id DESC` 복합 기준으로 충분하며, 커서 페이징만을 위해 UUID v7으로 변경할 필요는 없다.

---

## 11. 검증

다음 테스트를 갱신하거나 추가했다.

```text
MessageControllerTest
    cursor query parameter가 UUID로 바인딩되는지 검증

MessageServiceTest
    UUID cursor를 Repository에 전달하는지 검증
    hasNext=true이면 마지막 응답 Message id 문자열을 nextCursor로 넘기는지 검증

MessageRepositoryTest
    cursor가 null이면 첫 페이지를 createdAt DESC, id DESC로 조회하는지 검증
    cursor 메시지보다 오래된 메시지만 반환하는지 검증
    같은 createdAt에서는 id < cursorId 조건으로 다음 메시지를 이어 조회하는지 검증
    다른 채널 메시지 id를 cursor 위치로 사용하지 않는지 검증
    pageSize + 1 조회로 hasNext를 계산하는지 검증
    fetch join 계약이 유지되는지 검증
```

동일한 `createdAt` 검증은 테스트에서 세 메시지의 `createdAt`을 같은 값으로 맞춘 뒤, DB 정렬 기준인 `createdAt DESC, id DESC`에서 중간 메시지 ID를 cursor로 사용한다.

기대 결과는 다음과 같다.

```text
정렬 결과: [id-high, cursor-id, id-low]
다음 페이지 조건: createdAt = cursorCreatedAt and id < cursorId
조회 결과: [id-low]
```

이 테스트가 통과했다는 것은 `createdAt`만으로 자르면 누락될 수 있는 같은 시각 메시지를 `id` 보조 조건으로 이어서 조회한다는 의미다.

실행 명령은 다음과 같다.

```powershell
.\gradlew.bat --no-daemon test --tests "com.sprint.mission.discodeit.repository.MessageRepositoryTest" --tests "com.sprint.mission.discodeit.service.MessageServiceTest" --tests "com.sprint.mission.discodeit.controller.MessageControllerTest"
```

전체 테스트도 실행했다.

```powershell
.\gradlew.bat --no-daemon test
```

결과는 성공이다.

```text
BUILD SUCCESSFUL
```

---

## 최종 결론

이번 결정은 “UUID 단독 커서를 사용할 수 없다”가 아니다.

정확한 결론은 다음과 같다.

```text
UUID 타입과 UUID 생성 방식만으로는 메시지 생성 시간순을 안정적으로 보장할 수 없다.

createdAt 단독으로는 동일 시각 데이터가 누락될 수 있으므로
createdAt + UUID 복합 기준을 선택했다.

외부 API cursor는 UUID 단일 값으로 유지하지만,
내부 데이터베이스 페이징 경계는 createdAt + UUID 복합 기준이다.
```

현재 구현은 이 복합 기준을 Repository 서브쿼리에서 해석한다. 이 방식은 Service의 cursor 해석 로직을 단순하게 유지하고 Repository 호출 한 번으로 처리할 수 있다. 대신 잘못된 cursor를 명확한 예외로 구분하기 어렵고 Querydsl 조건이 복잡해지는 trade-off가 있다.
