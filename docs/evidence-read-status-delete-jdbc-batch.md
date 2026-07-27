# ReadStatus 채널 기준 삭제 JDBC batch 적용 증거

## 목적

`ReadStatusRepository.deleteByChannel_Id(...)`가 채널에 연결된 `ReadStatus`를 삭제할 때, 삭제 결과가 올바른지와 Hibernate JDBC batch가 실제로 적용되는지 확인한 evidence 문서다.

이 문서에서 다루는 핵심 질문은 다음과 같다.

```text
delete from read_statuses where id=? SQL이 두 번 보이는데,
이것이 DB에 개별 전송된 것인지 아니면 JDBC batch로 묶여 실행된 것인지 어떻게 판단할 수 있는가?
```

결론부터 정리하면, 아래 로그가 batch 실행의 핵심 근거다.

```text
Executing JDBC batch (2 / 50) - `com.sprint.mission.discodeit.entity.ReadStatus#DELETE`
```

위 문구는 `ReadStatus#DELETE` 작업 2건이 Hibernate JDBC batch에 모인 뒤, `PreparedStatement.executeBatch()`로 실행됐음을 의미한다.

---

## 관련 코드

대상 Repository 메서드는 다음과 같다.

```java
void deleteByChannel_Id(UUID channelId);
```

이 메서드는 `@Modifying @Query`로 작성한 JPQL bulk delete가 아니라 Spring Data JPA derived delete 메서드다.

따라서 기대 동작은 아래 bulk SQL 한 번이 아니다.

```sql
delete from read_statuses where channel_id = ?
```

실제 흐름은 다음에 가깝다.

```text
1. channel_id 조건에 해당하는 ReadStatus 엔티티를 먼저 조회한다.
2. 조회된 각 ReadStatus 엔티티를 삭제 대상으로 영속성 컨텍스트에 등록한다.
3. flush 시점에 엔티티 단위 DELETE SQL이 만들어진다.
4. Hibernate JDBC batch 설정이 적용되어 있으면 같은 DELETE 템플릿을 batch에 모아 실행한다.
```

이 때문에 SQL 템플릿은 row마다 아래처럼 보일 수 있다.

```sql
delete from read_statuses where id = ?
```

여기서 같은 DELETE 템플릿이 여러 번 로그에 보인다고 해서 반드시 DB로 여러 번 개별 전송됐다고 단정하면 안 된다. batch 적용 여부는 Hibernate batch trace 로그의 `Adding to JDBC batch`, `Executing JDBC batch` 문구로 판단한다.

---

## 관련 테스트

검증 대상 테스트는 다음이다.

```java
@Test
@DisplayName("채널별 읽음 상태 삭제 성공 - 채널에 연결된 읽음 상태 삭제")
void deleteByChannel_Id_deletesReadStatuses_whenChannelHasReadStatuses()
```

테스트 fixture는 다음 구조다.

```text
targetChannel
- savedTargetReadStatus: savedUser와 연결
- savedOtherTargetReadStatus: savedOtherUser와 연결

otherChannel
- savedRemainingReadStatus: savedUser와 연결
```

검증 의도는 다음과 같다.

```text
1. 삭제 전 targetChannel에는 ReadStatus 2건이 존재해야 한다.
2. 삭제 전 otherChannel에는 ReadStatus 1건이 존재해야 한다.
3. deleteByChannel_Id(targetChannelId)를 호출한다.
4. flush로 DELETE 실행 시점을 명확히 한다.
5. 삭제 후 targetChannel의 ReadStatus는 모두 없어야 한다.
6. 삭제 후 otherChannel의 ReadStatus는 그대로 남아야 한다.
7. 전체 ReadStatus count는 1이어야 한다.
```

즉, 테스트 assertion의 책임은 `deleteByChannel_Id(...)`가 삭제 조건을 정확히 적용하는지 검증하는 것이다.

---

## 설정

JDBC batch size는 공통 설정에 둔다.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
        jdbc:
          batch_size: 50
```

테스트 환경에서는 SQL과 batch trace를 확인할 수 있도록 다음 로그 레벨을 사용한다.

```yaml
logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.orm.jdbc.batch: trace
```

개발 환경도 같은 방식으로 SQL과 batch trace를 확인할 수 있게 설정했다.

```yaml
logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.orm.jdbc.batch: trace
```

운영 환경은 상세 SQL과 batch 내부 로그를 노출하지 않도록 `warn` 수준으로 둔다.

```yaml
logging:
  level:
    org.hibernate.SQL: warn
    org.hibernate.orm.jdbc.batch: warn
```

---

## 실행 명령

대상 테스트만 실행하려면 다음 명령을 사용한다.

```powershell
.\gradlew.bat --no-daemon test --tests "com.sprint.mission.discodeit.repository.ReadStatusRepositoryTest.deleteByChannel_Id_deletesReadStatuses_whenChannelHasReadStatuses" --info
```

batch 관련 로그만 좁혀 보려면 다음처럼 필터링할 수 있다.

```powershell
.\gradlew.bat --no-daemon test --tests "com.sprint.mission.discodeit.repository.ReadStatusRepositoryTest.deleteByChannel_Id_deletesReadStatuses_whenChannelHasReadStatuses" --info |
    Select-String -Pattern "Adding to JDBC batch|Executing JDBC batch|ReadStatus#DELETE|delete from read_statuses"
```

---

## 확인된 로그

실행 중 확인된 핵심 로그는 다음과 같다.

```text
TRACE org.hibernate.orm.jdbc.batch - Created Batch (50) - `com.sprint.mission.discodeit.entity.ReadStatus#DELETE`
TRACE org.hibernate.orm.jdbc.batch - Adding to JDBC batch (1) - `com.sprint.mission.discodeit.entity.ReadStatus#DELETE`
DEBUG org.hibernate.SQL -
    delete
    from
        read_statuses
    where
        id=?
TRACE org.hibernate.orm.jdbc.batch - Adding to JDBC batch (2) - `com.sprint.mission.discodeit.entity.ReadStatus#DELETE`
DEBUG org.hibernate.SQL -
    delete
    from
        read_statuses
    where
        id=?
TRACE org.hibernate.orm.jdbc.batch - Executing JDBC batch (2 / 50) - `com.sprint.mission.discodeit.entity.ReadStatus#DELETE`
```

각 로그의 의미는 다음과 같다.

```text
Created Batch (50)
- Hibernate가 batch size 50짜리 JDBC batch를 생성했다.

Adding to JDBC batch (1)
- 첫 번째 ReadStatus DELETE 작업이 batch에 추가됐다.

Adding to JDBC batch (2)
- 두 번째 ReadStatus DELETE 작업이 batch에 추가됐다.

delete from read_statuses where id=?
- 삭제 SQL 템플릿이다.
- 템플릿이 두 번 보이는 것은 삭제 대상 엔티티가 2건이기 때문이다.
- 이 로그만으로는 개별 전송인지 batch 실행인지 판단하지 않는다.

Executing JDBC batch (2 / 50)
- batch에 쌓인 DELETE 2건을 실행했다.
- 2는 이번 실행에서 batch에 들어간 작업 수다.
- 50은 hibernate.jdbc.batch_size 설정값이다.
- 이 문구가 실제 batch 실행 여부를 판단하는 핵심 근거다.
```

---

## 왜 테스트 assertion만으로는 batch 여부를 검증하지 않는가

아래 assertion은 삭제 결과를 검증한다.

```java
assertThat(statusesForTargetChannel).isEmpty();
assertThat(statusesForOtherChannel)
        .hasSize(1)
        .singleElement()
        .extracting(ReadStatus::getId, ReadStatus::getUserId, ReadStatus::getChannelId)
        .containsExactly(savedRemainingReadStatusId, savedUserId, savedOtherChannelId);
assertThat(readStatusRepository.count()).isEqualTo(1);
```

이 검증으로 알 수 있는 것은 다음이다.

```text
- targetChannel에 연결된 ReadStatus 2건이 삭제됐다.
- otherChannel에 연결된 ReadStatus 1건은 삭제되지 않았다.
- 최종적으로 read_statuses 테이블에 1건만 남았다.
```

하지만 이 assertion만으로는 아래 두 실행 방식을 구분할 수 없다.

```text
방식 1: executeUpdate()를 두 번 호출
방식 2: addBatch()를 두 번 호출한 뒤 executeBatch()를 한 번 호출
```

두 방식 모두 최종 DB 상태는 동일하기 때문이다.

따라서 Repository 테스트의 assertion은 삭제 결과와 조건을 검증하고, JDBC batch 적용 여부는 `org.hibernate.orm.jdbc.batch: trace` 로그로 확인하는 것이 현재 구조에서는 가장 현실적이다.

만약 batch 여부까지 테스트 assertion으로 강제하려면 별도 계측 구성이 필요하다.

예를 들면 다음과 같은 테스트 전용 JDBC proxy가 필요하다.

```text
Repository
-> Hibernate
-> DataSource proxy
-> Connection proxy
-> PreparedStatement proxy
-> 실제 H2 DataSource
```

이 프록시에서 `PreparedStatement.executeBatch()` 호출 수를 카운트한 뒤 다음처럼 검증할 수 있다.

```java
assertThat(batchCounter.getExecuteBatchCount()).isGreaterThan(0);
```

다만 이 방식은 Repository 동작 검증보다 Hibernate/JDBC 내부 실행 방식 검증에 가깝다. 테스트가 구현 디테일에 강하게 결합되므로, 현재 Repository 슬라이스 테스트에는 넣지 않는 편이 낫다.

---

## 추가로 확인해 보면 좋은 점

### 1. `saveAndFlush(...)`가 insert batch를 쪼개고 있는지

현재 테스트 fixture는 엔티티를 저장할 때 `saveAndFlush(...)`를 자주 사용한다.

```java
User savedUser = userRepository.saveAndFlush(new User(userCreateCommand, null));
User savedOtherUser = userRepository.saveAndFlush(new User(otherUserCreateCommand, null));
```

`saveAndFlush(...)`는 저장 직후 flush를 강제한다. 따라서 같은 타입의 insert가 여러 개 있어도 Hibernate가 충분히 모으기 전에 batch가 실행될 수 있다.

insert batch 자체를 확인하고 싶다면 다음처럼 구성해야 한다.

```java
userRepository.save(new User(firstCommand, null));
userRepository.save(new User(secondCommand, null));
em.flush();
```

다만 이번 evidence의 관심사는 insert가 아니라 `ReadStatus#DELETE` batch다. 따라서 현재 테스트에서 insert batch가 잘 묶이지 않는 것은 이 evidence의 결론에 영향을 주지 않는다.

### 2. 삭제 전 `em.clear()`가 왜 필요한지

삭제 전 `em.clear()`는 저장 직후 영속성 컨텍스트에 남아 있는 엔티티가 조회/삭제 검증에 영향을 주지 않게 한다.

```java
em.clear();
readStatusRepository.deleteByChannel_Id(savedTargetChannelId);
```

이 호출이 없으면 테스트가 실제 DB 조회와 삭제 흐름을 검증하는지, 이미 1차 캐시에 있는 엔티티 상태를 기준으로 통과하는지 흐려질 수 있다.

### 3. `deleteByChannel_Id(...)`의 derived delete가 먼저 SELECT를 수행하는지

Spring Data JPA derived delete는 삭제 대상 엔티티를 먼저 조회한 뒤 삭제 대상으로 등록하는 방식으로 동작할 수 있다.

따라서 아래 SELECT가 보이는 것은 이상한 현상이 아니다.

```sql
select ...
from read_statuses
where channel_id = ?
```

이 SELECT는 bulk delete를 쓰지 않는 대신 엔티티 삭제 흐름을 따르는 비용이다. bulk delete와 달리 영속성 컨텍스트, 엔티티 삭제 흐름, 연관 처리 측면에서 더 보수적인 선택이다.

### 4. batch size보다 삭제 대상 수가 적을 때도 batch가 실행되는지

현재 설정은 다음과 같다.

```yaml
hibernate:
  jdbc:
    batch_size: 50
```

삭제 대상은 2건뿐이다.

```text
Executing JDBC batch (2 / 50)
```

이 로그는 batch size가 50이어도 flush 시점에 현재 쌓인 2건을 batch로 실행한다는 뜻이다. batch size 50은 "50건이 될 때까지 반드시 기다린다"는 의미가 아니라, 한 batch에 담을 수 있는 최대 단위로 이해하면 된다.

### 5. 더 많은 삭제 건수에서 batch가 어떻게 나뉘는지

삭제 대상이 50건을 넘으면 batch가 여러 번 실행되는지 확인해 볼 수 있다.

예를 들어 삭제 대상이 120건이고 batch size가 50이면 기대 로그는 개념적으로 다음과 같다.

```text
Executing JDBC batch (50 / 50)
Executing JDBC batch (50 / 50)
Executing JDBC batch (20 / 50)
```

현재 테스트는 최소 evidence로 2건 삭제만 사용한다. batch 분할까지 검증하고 싶다면 별도의 성능/진단용 테스트를 두는 편이 좋다.

### 6. `order_updates` 설정이 필요한 상황인지

Hibernate에는 batch 효율을 높이기 위한 정렬 설정도 있다.

```yaml
hibernate:
  order_inserts: true
  order_updates: true
```

삭제 작업에는 이름 그대로 직접적인 delete 정렬 설정은 아니지만, insert/update batch를 더 안정적으로 묶고 싶을 때 같이 검토할 수 있다.

현재 evidence에서는 `ReadStatus#DELETE` 2건이 이미 같은 SQL 템플릿으로 모여 실행되는 것을 확인했으므로, 이 설정은 필수 확인 사항은 아니다.

### 7. 운영 로그 레벨은 trace로 두지 않는지

`org.hibernate.orm.jdbc.batch: trace`는 batch 진단에는 유용하지만 로그가 매우 많아진다.

운영 환경에서는 현재처럼 상세 SQL과 batch 내부 로그를 낮추지 않는 편이 좋다.

```yaml
logging:
  level:
    org.hibernate.SQL: warn
    org.hibernate.orm.jdbc.batch: warn
```

개발/test에서만 trace로 확인하고, 운영에서는 필요한 기간에만 일시적으로 올리는 방식이 안전하다.

### 8. P6Spy와 Hibernate SQL 로그가 중복 출력되는지

현재 test/dev에서는 P6Spy 로그와 Hibernate SQL 로그를 함께 볼 수 있다.

이 설정은 batch 진단에는 편하지만, SQL이 중복으로 보일 수 있다.

```yaml
logging:
  level:
    p6spy: info
    org.hibernate.SQL: debug
```

평상시 개발 로그가 과하게 많다면 둘 중 하나를 낮추고, batch 분석이 필요할 때만 `org.hibernate.SQL`과 `org.hibernate.orm.jdbc.batch`를 올리는 방식도 가능하다.

### 9. 테스트 assertion으로 batch를 강제해야 하는 요구사항인지

batch 적용은 현재 Repository의 기능 요구사항이라기보다 성능/진단 성격이 강하다.

따라서 일반 Repository 슬라이스 테스트에서는 다음을 검증하는 것이 더 적절하다.

```text
- 삭제 대상 채널의 ReadStatus가 모두 삭제된다.
- 비대상 채널의 ReadStatus는 삭제되지 않는다.
- 최종 row 수가 기대와 일치한다.
```

batch 실행 여부까지 실패 조건으로 삼아야 한다면, 그때는 Repository 테스트가 아니라 별도의 성능/인프라 진단 테스트로 분리하는 것이 좋다.

---

## 결론

이번 테스트에서 삭제 결과는 assertion으로 검증한다.

```text
targetChannel ReadStatus 2건 삭제
otherChannel ReadStatus 1건 유지
최종 ReadStatus count 1
```

JDBC batch 적용 여부는 Hibernate batch trace 로그로 확인한다.

판단 기준은 다음 문구다.

```text
Executing JDBC batch (2 / 50) - `com.sprint.mission.discodeit.entity.ReadStatus#DELETE`
```

따라서 이번 실행에서는 `ReadStatus#DELETE` 2건이 JDBC batch로 묶여 실행됐다고 판단할 수 있다.
