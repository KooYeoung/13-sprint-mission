# ConcurrentHashMap 동시성 원리 정리

## 1. PR 리뷰 내용

리뷰에서 받은 핵심 질문은 다음과 같다.

> Map으로 가져오게 변경하셨군요.  
> 그런데 멀티쓰레딩 환경에서도 동시성을 지키기 위해서 ConcurrentHashMap을 사용하셨네요.  
> 이 동기화를 구현하는 원리를 아시나요?  
> 이 코드를 보면 면접관이 질문할 수도 있어요.

현재 코드에서 리뷰 대상이 된 부분은 다음이다.

```java
private final Map<UUID, T> dataMap = new ConcurrentHashMap<>();
```

`FileObjectStorage`는 디스크의 `.ser` 파일을 읽어 메모리에 올려두고, 이후 조회 시 파일을 매번 읽지 않고 `dataMap`에서 데이터를 가져오는 구조다.

```java
public T load(UUID id) {
    T t = dataMap.get(id);
    return t;
}

public List<T> loadAll() {
    return dataMap.values().stream().toList();
}
```

따라서 `dataMap`은 단순한 지역 변수가 아니라, 저장소 내부에서 공유되는 메모리 캐시 역할을 한다.

---

## 2. 왜 HashMap이 아니라 ConcurrentHashMap을 사용했는가

Spring Bean은 기본적으로 singleton scope이다. 즉, 하나의 Bean 인스턴스가 Spring 컨테이너에 의해 공유된다.

현재 `FileObjectStorage` 자체가 직접 `@Repository`로 등록된 Bean은 아니더라도, `FileUserRepository` 같은 Repository Bean의 필드로 생성되어 보관된다면 결과적으로 여러 요청 스레드가 같은 `FileObjectStorage` 인스턴스에 접근할 수 있다.

예를 들면 다음 상황이 동시에 발생할 수 있다.

```text
Thread A: save(entity)
Thread B: load(id)
Thread C: delete(id)
Thread D: loadAll()
```

일반 `HashMap`은 여러 스레드가 동시에 `put`, `remove`, `get`, iteration을 수행할 때 안전하지 않다. Map 내부 구조가 깨지거나, 동시 수정 중 조회 결과가 일관되지 않을 수 있다.

그래서 여러 요청 스레드가 동시에 접근할 가능성이 있는 `dataMap`에는 `HashMap`이 아니라 `ConcurrentHashMap`을 사용했다.

정리하면 다음과 같다.

```text
HashMap
- 멀티스레드 환경에서 안전하지 않음
- 동시 put/remove/iteration 시 문제가 생길 수 있음

ConcurrentHashMap
- 멀티스레드 환경에서 사용할 수 있는 Map 구현체
- get/put/remove 같은 개별 연산이 thread-safe
- 전체 Map을 하나의 lock으로 막지 않고 높은 동시성을 제공
```

---

## 3. ConcurrentHashMap의 동기화 구현 원리

`ConcurrentHashMap`은 모든 메서드에 단순히 `synchronized`를 거는 방식이 아니다.

핵심은 다음과 같다.

```text
1. 조회 작업은 대부분 lock 없이 수행한다.
2. 수정 작업은 전체 Map이 아니라 필요한 bin/bucket 단위로 제어한다.
3. 빈 bin에 처음 삽입하는 경우 CAS를 사용한다.
4. 충돌이 있는 bin에서는 해당 bin의 첫 번째 노드를 lock처럼 사용한다.
5. 따라서 서로 다른 bin에 접근하는 작업은 동시에 진행될 수 있다.
```

Oracle Java 17 공식 문서 기준으로 `ConcurrentHashMap`은 retrieval operation, 즉 `get` 같은 조회 작업이 일반적으로 block되지 않고, `put`, `remove` 같은 update operation과 겹쳐 수행될 수 있다고 설명한다.

또한 특정 key에 대한 update operation은 그 update 값을 읽는 retrieval과 happens-before 관계를 가진다. 쉽게 말하면, 어떤 스레드가 `put(id, entity)`를 완료했고 다른 스레드가 같은 `id`로 그 값을 읽었다면, 그 update 결과를 안전하게 볼 수 있도록 설계되어 있다는 의미다.

### 3.1 get은 보통 lock 없이 수행된다

현재 코드의 조회 메서드는 다음과 같다.

```java
public T load(UUID id) {
    return dataMap.get(id);
}
```

`ConcurrentHashMap`의 `get`은 일반적으로 lock을 잡지 않는다. 그래서 한 스레드가 `put` 또는 `remove` 중이더라도 다른 스레드의 `get`이 전체 Map lock 때문에 막히지 않는다.

```text
Thread A: put(id1, entity1)
Thread B: get(id2)
Thread C: get(id1)

=> 조회 작업은 일반적으로 block되지 않고 진행 가능
```

물론 `get(id1)`이 항상 “가장 최신 변경 중간 상태”를 본다는 뜻은 아니다. 완료된 update를 기준으로 안전하게 보이는 값을 읽는다고 이해하는 것이 좋다.

### 3.2 put은 CAS 또는 bin 단위 lock을 사용한다

`ConcurrentHashMap`은 내부적으로 배열 table과 bin/bucket 구조를 사용한다.

`put`을 할 때 key의 hash를 계산하고, 해당 key가 들어갈 bin 위치를 찾는다.

```text
1. key의 hash 계산
2. table에서 들어갈 bin 위치 계산
3. 해당 bin이 비어 있으면 CAS로 첫 노드 삽입
4. 이미 노드가 있으면 해당 bin에 대해서만 lock 사용
5. 같은 bin 안에서 insert/delete/replace 수행
```

OpenJDK 소스 주석 기준으로, 빈 bin에 첫 번째 노드를 삽입하는 경우에는 CAS로 처리한다. CAS는 “현재 값이 내가 예상한 값과 같으면 새 값으로 바꾼다”는 원자적 연산이다.

반대로 이미 해당 bin에 노드가 있다면 update 작업에는 lock이 필요하다. 이때 별도의 lock 객체를 bin마다 두는 것이 아니라, bin list의 첫 번째 노드를 lock처럼 사용하고 Java의 내장 `synchronized` monitor를 활용한다.

즉, `ConcurrentHashMap`은 전체 Map을 하나로 잠그는 것이 아니라, 필요한 bin 단위로만 잠근다.

```text
Thread A: bin 1 수정
Thread B: bin 8 수정
Thread C: bin 13 조회

=> 전체 Map lock이 아니므로 동시에 처리 가능
```

이것이 `Collections.synchronizedMap(...)`이나 예전 `Hashtable`보다 동시성 측면에서 유리한 이유다.

---

## 4. Java 8 이후 구현 기준으로 이해하기

예전 자료에서는 `ConcurrentHashMap`을 설명할 때 “Segment 단위 lock”이라는 표현이 자주 나온다.

하지만 현재 프로젝트에서 사용하는 Java 17 기준으로는 `Segment` 중심으로 설명하기보다 다음처럼 설명하는 것이 더 적절하다.

```text
Java 8 이후 ConcurrentHashMap 설명 포인트
- table/bin 기반 구조
- get은 일반적으로 lock-free
- 빈 bin 삽입은 CAS
- 충돌이 있는 bin의 update는 bin 첫 노드를 synchronized monitor로 lock
- 전체 Map lock은 사용하지 않음
```

면접에서 “Segment lock 아닌가요?”라는 질문이 나오면 다음처럼 답할 수 있다.

> 예전 Java 7까지의 설명에서는 Segment 단위 lock 구조가 많이 언급됩니다. 하지만 Java 8 이후 구현에서는 table/bin 기반으로 설명하는 것이 더 정확하다고 이해했습니다. 조회는 일반적으로 lock 없이 수행되고, 빈 bin 삽입은 CAS를 사용하며, 충돌이 있는 bin의 update는 bin의 첫 노드를 lock처럼 사용합니다.

---

## 5. ConcurrentHashMap이 보장하는 것

`ConcurrentHashMap`이 보장하는 것은 주로 Map의 개별 연산에 대한 thread-safe다.

현재 코드 기준으로 보면 다음 연산들은 Map 내부 구조를 안전하게 다룬다.

```java
dataMap.get(id);
dataMap.put(entityId, entity);
dataMap.remove(id);
dataMap.values();
```

정리하면 다음과 같다.

```text
보장하는 것
- get/put/remove 같은 개별 Map 연산의 thread-safe
- Map 내부 구조의 일관성
- update 완료 후 같은 key를 조회하는 작업에서의 메모리 가시성
- 동시 수정 중 iterator 사용 시 ConcurrentModificationException을 던지지 않는 weakly consistent iterator
```

특히 `ConcurrentHashMap`의 iterator는 일반 `HashMap`과 다르게 동시 수정 중이라고 해서 `ConcurrentModificationException`을 던지지 않는다.

그래서 현재 코드의 `loadAll()`도 동시 수정 중 예외가 터질 가능성은 낮다.

```java
public List<T> loadAll() {
    return dataMap.values().stream().toList();
}
```

다만 이것은 “완벽한 한 시점의 snapshot”을 보장한다는 뜻은 아니다. 동시 `save` 또는 `delete`가 진행 중이라면 `loadAll()` 결과는 어느 한 시점 또는 그 이후의 상태 일부를 반영할 수 있다.

---

## 6. ConcurrentHashMap이 보장하지 않는 것

이 부분이 가장 중요하다.

`ConcurrentHashMap`을 사용한다고 해서 모든 동시성 문제가 자동으로 해결되는 것은 아니다.

### 6.1 여러 Map 연산을 조합한 로직 전체는 자동으로 원자적이지 않다

다음 코드는 안전하지 않을 수 있다.

```java
if (!dataMap.containsKey(id)) {
    dataMap.put(id, entity);
}
```

두 스레드가 동시에 실행되면 둘 다 `containsKey(id)`를 `false`로 보고 둘 다 `put`할 수 있다.

이런 경우에는 다음과 같은 atomic 메서드를 사용해야 한다.

```java
dataMap.putIfAbsent(id, entity);
dataMap.computeIfAbsent(id, key -> entity);
dataMap.compute(id, (key, oldValue) -> newValue);
dataMap.merge(id, entity, (oldValue, newValue) -> newValue);
```

즉, `ConcurrentHashMap`은 개별 메서드 호출은 안전하게 만들어주지만, 여러 메서드를 조합한 비즈니스 로직 전체를 자동으로 하나의 원자적 작업으로 묶어주지는 않는다.

### 6.2 파일 작업과 Map 작업을 transaction처럼 묶어주지 않는다

현재 `save` 메서드는 다음과 같다.

```java
public void save(T entity) {
    UUID entityId = entity.getId();
    Path filePath = getFilePath(entityId);

    try (
            FileOutputStream fos = new FileOutputStream(filePath.toFile());
            ObjectOutputStream oos = new ObjectOutputStream(fos)
    ) {
        oos.writeObject(entity);
        dataMap.put(entityId, entity);
    } catch (IOException e) {
        throw new CustomInternalServerException(FileError.SAVE.getMessage(), e);
    }
}
```

이 코드는 순서상 다음 장점이 있다.

```text
1. 파일 저장 성공
2. dataMap 갱신
```

파일 저장이 실패하면 `dataMap.put(...)`이 실행되지 않으므로, “파일 저장은 실패했는데 메모리 Map에는 들어간 상태”를 어느 정도 방지한다.

하지만 `ConcurrentHashMap`이 보장하는 것은 Map 자체의 동시성이다. 파일 저장과 `dataMap.put(...)`을 합친 전체 작업을 하나의 transaction처럼 보장해주지는 않는다.

예를 들어 다음 상황은 여전히 고려 대상이다.

```text
Thread A: save(id=1)로 파일 쓰는 중
Thread B: delete(id=1) 실행
Thread C: load(id=1) 실행
```

이 경우 `dataMap`의 `put/remove/get` 자체는 안전하지만, 디스크 파일 상태와 메모리 Map 상태가 항상 하나의 원자적 상태로 함께 움직인다고 볼 수는 없다.

### 6.3 Map 안의 value 객체 자체의 thread-safe는 별개다

`ConcurrentHashMap<UUID, T>`는 Map 구조를 thread-safe하게 보호한다.

하지만 Map 안에 들어있는 `T entity` 객체 자체가 mutable하다면, 그 객체 내부 필드 변경까지 자동으로 thread-safe해지는 것은 아니다.

예를 들어 다음과 같은 코드가 있다고 가정한다.

```java
T entity = dataMap.get(id);
entity.updateName("newName");
```

여러 스레드가 같은 `entity` 객체를 꺼내서 동시에 내부 필드를 수정한다면, 그 필드 변경의 동시성은 `ConcurrentHashMap`이 보호해주지 않는다.

즉, `ConcurrentHashMap`은 다음을 보장한다.

```text
Map에 넣기, 찾기, 제거하기의 구조적 동시성
```

하지만 다음은 보장하지 않는다.

```text
Map 안에 들어있는 객체 내부 상태의 thread-safe
파일 시스템 작업과 Map 작업의 트랜잭션적 일관성
여러 Map 연산을 조합한 비즈니스 로직 전체의 원자성
```

---

## 7. 현재 코드에 대한 분석

### 7.1 save

```java
public void save(T entity) {
    UUID entityId = entity.getId();
    Path filePath = getFilePath(entityId);

    try (
            FileOutputStream fos = new FileOutputStream(filePath.toFile());
            ObjectOutputStream oos = new ObjectOutputStream(fos)
    ) {
        oos.writeObject(entity);
        dataMap.put(entityId, entity);
    } catch (IOException e) {
        throw new CustomInternalServerException(FileError.SAVE.getMessage(), e);
    }
}
```

좋은 점은 파일 저장이 성공한 뒤에만 `dataMap.put(...)`을 한다는 것이다.

주의할 점은 파일 저장과 Map 갱신이 하나의 원자적 작업은 아니라는 것이다.

### 7.2 load

```java
public T load(UUID id) {
    T t = dataMap.get(id);
    return t;
}
```

`ConcurrentHashMap.get(...)`을 사용하므로 Map 조회 자체는 thread-safe하다.

다만 반환된 entity 객체가 mutable하다면, 그 객체를 외부에서 수정할 때의 thread-safe는 별도로 고려해야 한다.

### 7.3 loadAll

```java
public List<T> loadAll() {
    return dataMap.values().stream().toList();
}
```

동시 수정 중에도 `ConcurrentModificationException`은 발생하지 않는다.

하지만 동시 `save/delete`가 진행 중이라면 `loadAll()` 결과가 정확히 한 시점의 완벽한 snapshot이라고 보장되지는 않는다.

현재 미션 수준에서는 대부분 문제가 되지 않을 수 있지만, 면접에서는 weakly consistent iterator라는 특징을 함께 설명하면 좋다.

### 7.4 delete

```java
public void delete(UUID id) {
    Path filePath = getFilePath(id);
    try {
        Files.deleteIfExists(filePath);
    } catch (IOException e) {
        throw new CustomInternalServerException(FileError.DELETE.getMessage(), e);
    }
    dataMap.remove(id);
}
```

파일 삭제가 성공한 뒤에 `dataMap.remove(...)`를 수행한다.

Map 삭제 자체는 thread-safe하지만, 파일 삭제와 Map 삭제가 하나의 원자적 작업은 아니다.

---

## 8. 개선이 필요하다면 고려할 수 있는 방법

현재 PR 수준에서는 `ConcurrentHashMap`을 사용한 것 자체는 좋은 접근이다.

다만 더 엄격한 일관성이 필요한 요구사항이라면 다음 개선을 고려할 수 있다.

### 8.1 파일 작업과 Map 갱신 구간에 lock 사용

파일 작업과 Map 갱신을 하나의 임계 영역으로 묶고 싶다면 `ReentrantReadWriteLock` 같은 lock을 사용할 수 있다.

```java
private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

public void save(T entity) {
    lock.writeLock().lock();
    try {
        UUID entityId = entity.getId();
        Path filePath = getFilePath(entityId);

        try (
                FileOutputStream fos = new FileOutputStream(filePath.toFile());
                ObjectOutputStream oos = new ObjectOutputStream(fos)
        ) {
            oos.writeObject(entity);
            dataMap.put(entityId, entity);
        } catch (IOException e) {
            throw new CustomInternalServerException(FileError.SAVE.getMessage(), e);
        }
    } finally {
        lock.writeLock().unlock();
    }
}
```

다만 이 방식은 `ConcurrentHashMap`의 장점인 높은 동시성을 일부 줄일 수 있다.

### 8.2 파일 저장 시 임시 파일 사용 후 교체

파일 저장 중간에 장애가 발생했을 때 깨진 파일이 남는 것을 줄이고 싶다면 다음 방식도 고려할 수 있다.

```text
1. 임시 파일에 먼저 write
2. write 성공 시 실제 파일명으로 move/replace
3. 그 후 dataMap 갱신
```

예시 방향은 다음과 같다.

```java
Path tempPath = path.resolve(entityId + ".tmp");
Path filePath = getFilePath(entityId);

// tempPath에 먼저 저장
// 성공하면 Files.move(tempPath, filePath, REPLACE_EXISTING, ATOMIC_MOVE)
// 이후 dataMap.put(entityId, entity)
```

단, `ATOMIC_MOVE`는 파일 시스템에 따라 지원 여부가 다를 수 있으므로 예외 처리가 필요하다.

### 8.3 현재 미션에서는 과도하게 복잡하게 만들 필요는 없음

현재 미션의 핵심이 파일 저장소 구현과 메모리 캐시 구조라면, 지금처럼 `ConcurrentHashMap`을 사용하고 그 한계를 설명할 수 있는 정도면 충분해 보인다.

즉, 이번 PR에서는 다음 방향이 적절하다.

```text
- ConcurrentHashMap 사용 유지
- 왜 사용했는지 주석 또는 evidence에 정리
- 파일 작업과 Map 갱신은 완전한 transaction이 아니라는 한계 인지
- 더 강한 일관성이 필요하면 lock 또는 임시 파일 교체 방식 고려
```

---

## 9. 내용정리

```md
`FileObjectStorage`는 Repository 내부에서 공유되는 저장소이고,
Spring Bean은 기본적으로 singleton으로 관리되기 때문에 여러 요청 스레드가 동시에 접근할 수 있다고 판단했습니다.

그래서 내부 캐시 역할을 하는 `dataMap`은 일반 `HashMap`이 아니라 `ConcurrentHashMap`을 사용했습니다.

`ConcurrentHashMap`은 전체 Map에 하나의 lock을 거는 방식이 아니라,
조회 작업은 일반적으로 lock 없이 수행하고, 수정 작업은 필요한 bin/bucket 단위에서 CAS 또는 부분 lock을 사용해 동시성을 보장하는 구조로 이해했습니다.

Java 17 기준으로 빈 bin에 첫 노드를 삽입할 때는 CAS를 사용하고,
충돌이 있는 bin에서 update가 필요한 경우에는 bin의 첫 번째 노드를 lock처럼 사용하며 `synchronized` monitor를 활용합니다.

다만 `ConcurrentHashMap`이 보장하는 것은 Map의 개별 연산에 대한 thread-safe이고,
현재 코드의 파일 저장/삭제 작업과 `dataMap` 갱신을 하나의 원자적 작업으로 묶어주는 것은 아닙니다.

따라서 더 엄격한 파일-메모리 일관성이 필요하다면 파일 작업과 Map 갱신 구간을 별도 lock으로 보호하거나,
임시 파일에 먼저 저장한 뒤 실제 파일로 교체하는 방식도 고려할 수 있다고 정리했습니다.
```

---

## 10. 면접 답변 예시

면접에서 “ConcurrentHashMap이 어떻게 동시성을 보장하나요?”라고 질문받으면 다음처럼 답변할 수 있다.

```text
ConcurrentHashMap은 멀티스레드 환경에서 안전하게 사용할 수 있는 Map 구현체입니다.
일반 HashMap은 여러 스레드가 동시에 put, remove, get을 수행하면 내부 구조의 일관성이 깨질 수 있기 때문에 공유 캐시로 사용하기 어렵습니다.

ConcurrentHashMap은 전체 Map에 하나의 lock을 거는 방식이 아닙니다.
조회 작업인 get은 일반적으로 lock 없이 수행되고, put이나 remove 같은 update 작업은 필요한 bucket/bin 단위에서만 제어됩니다.
Java 8 이후 구현 기준으로는 빈 bin에 첫 노드를 넣을 때 CAS를 사용하고, 충돌이 있는 bin에서는 bin의 첫 번째 노드를 lock처럼 사용해 synchronized monitor로 동기화합니다.

그래서 서로 다른 bin에 접근하는 작업은 동시에 처리될 수 있고, 전체 Map을 막는 synchronizedMap이나 Hashtable보다 동시성이 좋습니다.

다만 ConcurrentHashMap이 모든 로직을 원자적으로 만들어주는 것은 아닙니다.
get 후 put처럼 여러 연산을 조합하면 race condition이 생길 수 있으므로 putIfAbsent, computeIfAbsent, compute 같은 atomic 메서드를 사용해야 합니다.
또한 현재 코드처럼 파일 저장과 Map 갱신이 함께 있는 경우 ConcurrentHashMap은 Map 자체의 동시성만 보장하고 파일 작업까지 transaction처럼 묶어주지는 않기 때문에, 더 엄격한 일관성이 필요하면 별도 lock이 필요합니다.
```

---

## 11. 결론

이번 코드에서 `ConcurrentHashMap`을 사용한 이유는 다음과 같이 정리할 수 있다.

```text
1. Repository는 Spring 환경에서 여러 요청 스레드가 동시에 접근할 수 있다.
2. dataMap은 파일 저장소의 메모리 캐시 역할을 하는 공유 상태다.
3. 일반 HashMap은 동시 접근에 안전하지 않다.
4. ConcurrentHashMap은 get/put/remove 같은 개별 Map 연산을 thread-safe하게 처리한다.
5. 내부적으로 전체 Map lock이 아니라 lock-free read, CAS, bin 단위 lock을 활용한다.
6. 다만 파일 작업과 Map 갱신 전체를 transaction처럼 묶어주지는 않으므로 그 한계는 인지해야 한다.
```

따라서 현재 PR에 대한 답변은 다음 한 문장으로 요약할 수 있다.

> `ConcurrentHashMap`은 공유 캐시인 `dataMap`에 여러 요청 스레드가 동시에 접근할 수 있기 때문에 사용했고, 내부적으로는 조회는 일반적으로 lock 없이 수행하며 수정은 CAS와 bin 단위 lock을 통해 동시성을 보장합니다. 다만 Map 자체의 동시성만 보장하므로 파일 I/O와 Map 갱신의 원자성은 별도 고려가 필요합니다.

