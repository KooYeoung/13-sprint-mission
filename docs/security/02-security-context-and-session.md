# SecurityContext와 Session

## 1. 세 객체의 관계

Spring Security에서 현재 인증 정보를 이해할 때 다음 관계를 기준으로 본다.

```text
SecurityContextHolder
        ↓
SecurityContext
        ↓
Authentication
```

- `Authentication`: 현재 사용자가 누구이고 어떤 권한을 가지고 있는지를 표현한다.
- `SecurityContext`: 현재 인증된 `Authentication`을 보관한다.
- `SecurityContextHolder`: 현재 실행 흐름에서 `SecurityContext`에 접근할 수 있도록 한다.

예를 들면 다음과 같이 현재 인증정보를 가져올 수 있다.

```java
Authentication authentication =
        SecurityContextHolder.getContext().getAuthentication();
```

---

## 2. SecurityContextHolder와 ThreadLocal

기본적인 Servlet 환경에서는 `SecurityContextHolder`가 현재 스레드와 SecurityContext를 연결해서 관리하는 방식으로 이해할 수 있다.

```text
Thread-A
→ SecurityContextHolder
→ SecurityContext
→ Authentication
```

이 때문에 요청이 끝난 뒤 현재 스레드에 남아 있는 SecurityContext를 정리하는 것이 중요하다.

WAS는 요청마다 새로운 스레드를 계속 만드는 것이 아니라 스레드 풀의 기존 스레드를 재사용할 수 있다.

```text
사용자 A 요청
→ Thread-1
→ 사용자 A의 Authentication

요청 종료 후 Context를 정리하지 않음

사용자 B 요청
→ 같은 Thread-1 재사용
→ 사용자 A의 인증정보가 남을 위험
```

따라서 요청 단위의 SecurityContext와 여러 요청 사이의 로그인 상태 저장을 구분해야 한다.

---

## 3. 세션 기반 로그인 상태 유지

최초 로그인에서 인증이 성공하면 `Authentication`이 `SecurityContext`에 저장되고, 세션 기반에서는 이 인증 상태를 서버 Session을 통해 다음 요청에서도 사용할 수 있다.

```text
최초 로그인
→ 인증 성공
→ Authentication
→ SecurityContext
→ Session을 통해 인증 상태 유지
```

클라이언트는 이후 요청에서 일반적으로 `JSESSIONID` 같은 세션 식별자를 전달한다.

```text
다음 요청
→ JSESSIONID
→ 서버의 Session 조회
→ 기존 SecurityContext 복원
→ 현재 요청의 SecurityContextHolder에 설정
→ 기존 Authentication 사용
→ 인가
```

중요한 점은 **SecurityContext를 복원하는 것과 새로운 인증을 수행하는 것은 다르다**는 것이다.

세션 기반에서 이미 로그인한 사용자는 일반적으로 매 API 요청마다 username/password를 다시 검증하지 않는다.

---

## 4. 요청 종료 후에도 로그인이 유지되는 이유

요청이 끝날 때 ThreadLocal의 SecurityContext를 정리해도 로그인 상태가 사라지는 것은 아니다.

```text
SecurityContextHolder(ThreadLocal)
→ 현재 요청 동안 사용
→ 요청 종료 시 정리

Session
→ 여러 HTTP 요청 사이에서 인증 상태 유지
```

다음 요청이 들어오면 세션에 저장된 인증 상태를 이용해 다시 현재 요청의 SecurityContextHolder를 구성할 수 있다.

---

## 5. AnonymousAuthenticationToken

Spring Security의 익명 인증 기능이 활성화되어 있고, 정상적인 인증 정보가 없는 요청이라면 익명 사용자를 표현하기 위해 `AnonymousAuthenticationToken`이 사용될 수 있다.

이 객체의 목적은 모든 코드에서 매번 `Authentication == null`을 처리하는 부담을 줄이고 익명 사용자도 일관된 Security 모델 안에서 다룰 수 있게 하는 것이다.

다만 다음 판단은 안전하지 않다.

```java
if (authentication != null && authentication.isAuthenticated()) {
    // 실제 로그인 사용자라고 단정
}
```

익명 Authentication도 `Authentication` 구현체이기 때문에 단순 null 여부나 `isAuthenticated()`만으로 실제 로그인 사용자를 판별하면 안 된다.

인가 기능에서는 익명 사용자와 실제 인증 사용자를 구분해서 처리한다.

---

## 6. 같은 세션의 동시 요청

동일 사용자가 브라우저에서 동시에 여러 API를 호출하면 서로 다른 요청 스레드가 같은 세션을 사용할 수 있다.

각 스레드의 ThreadLocal은 서로 별개지만, 세션에서 가져온 `SecurityContext` 인스턴스는 공유될 가능성이 있으므로 요청 처리 중 인증 객체를 임의로 수정하는 방식은 주의해야 한다.

```text
HttpSession
   ↓
SecurityContext
  ↑       ↑
Thread-A  Thread-B
```

인증 상태를 바꿔야 한다면 요청 중 공유 객체를 임의로 수정하기보다 재인증, 세션 무효화, 새로운 SecurityContext 구성 등 의도가 분명한 흐름을 고려한다.

---

## 7. 비동기 처리와 SecurityContext

`@Async`나 `CompletableFuture`는 다른 스레드에서 실행될 수 있다.

```text
요청 Thread-A
→ SecurityContext 있음

비동기 실행
→ Thread-B
→ 기존 ThreadLocal의 SecurityContext가 자동으로 따라간다고 가정하면 안 됨
```

비동기 작업에서 현재 사용자 인증 정보가 필요하다면 SecurityContext 전파 전략을 별도로 확인해야 한다.

이 주제는 다음 Java 비동기 학습에서 다시 Deep Dive한다.

---

## 8. 로그아웃

세션 기반 로그아웃에서는 클라이언트 쿠키만 제거하는 것보다 서버의 기존 세션을 더 이상 사용할 수 없게 만드는 것이 중요하다.

현재 프로젝트의 `SecurityConfig`는 다음을 수행한다.

```text
POST /api/auth/logout
→ Session 무효화
→ JSESSIONID 삭제
→ 204 No Content
```

코드에서는 `invalidateHttpSession(true)`와 `deleteCookies("JSESSIONID")`를 사용한다.

---

## 9. Session 방식과 Stateless 방식의 차이

```text
Session 기반
→ 인증 상태를 서버 Session으로 유지
→ 다음 요청에서 기존 SecurityContext 복원

Stateless 인증
→ 서버 Session에 로그인 인증 상태를 유지하지 않음
→ 요청마다 전달된 인증정보로 Authentication을 구성
→ 요청 처리 중에는 여전히 SecurityContext가 필요할 수 있음
```

Stateless라고 해서 `SecurityContext`라는 개념 자체가 사라지는 것은 아니다. 차이는 인증 상태를 다음 요청까지 서버 세션에 보존하는지 여부다.
