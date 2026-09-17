# Spring Security Deep Dive 기록

이 문서는 2026년 9월 16일 구두 학습에서 실제로 다룬 확장 주제를 보관한다.

시간을 들여 확인한 내용이므로 문서에서 제외하지 않는다. 다만 아래 내용은 현재 Discodeit 코드에 모두 구현됐다는 뜻이 아니다. 프로젝트에서 확인된 동작은 각 문서에서 따로 표시하고, 이 문서에서는 이후 설계·검증 후보를 정리한다.

```text
기본 흐름 이해
→ 실제 프로젝트 적용
→ 테스트
→ 구두 설명
→ Deep Dive 기록
→ 필요할 때 재현·구현
```

핵심 학습 범위와 심화 내용을 구분하는 목적은 학습 기록을 버리는 것이 아니라, 현재 미션의 완료 조건과 이후 탐구 대상을 혼동하지 않기 위해서다.

---

## 1. 비동기와 SecurityContext

### 확인한 문제

`SecurityContextHolder`의 기본 전략은 현재 실행 Thread와 `SecurityContext`를 연결한다.

따라서 다른 Thread에서 실행되는 `@Async`, `CompletableFuture`, 별도 `Executor` 작업은 요청 Thread의 인증 정보를 자동으로 사용할 수 있다고 가정하면 안 된다.

```text
요청 Thread-A
→ SecurityContext 있음

비동기 Thread-B
→ 별도 전파가 없으면 요청의 SecurityContext 없음
```

### 전파가 정말 필요한 경우

Spring Security는 다음과 같은 래퍼를 제공한다.

- `DelegatingSecurityContextRunnable`
- `DelegatingSecurityContextCallable`
- `DelegatingSecurityContextExecutor`
- `DelegatingSecurityContextAsyncTaskExecutor`

이들은 명시적으로 지정했거나 구성·제출 과정에서 캡처한 SecurityContext를 작업 실행 전에 설정하고, 실행이 끝나면 정리하는 책임을 묶는다. 정확한 캡처 시점은 사용하는 생성자와 위임 객체 구성에 따라 확인해야 한다. 직접 ThreadLocal에 값을 넣고 지우는 방식보다 누락 위험을 줄일 수 있다.

단, Context를 전달한다고 문제가 끝나는 것은 아니다. 제출 시점의 권한을 사용할지, 실제 실행 시점에 DB에서 최신 권한을 다시 확인할지는 업무 정책으로 결정해야 한다.

### ID만 전달할지 Context를 전달할지

비동기 작업에 사용자 ID만 필요하다면 ID를 명시적으로 인자로 전달하는 편이 의존성과 관리 범위가 작다.

SecurityContext 전체 전파는 다음과 같이 실제 인증 정보가 필요한 경우에 고려한다.

- 하위 호출이 현재 `Authentication`을 요구한다.
- 감사 정보가 현재 Principal과 Authority를 필요로 한다.
- Method Security가 비동기 Thread에서도 동작해야 한다.

권한 변경 가능성이 중요한 작업이라면 전달된 Authority만 신뢰하지 않고 실행 시점에 최신 권한이나 업무 상태를 다시 확인할 수 있다.

이 주제는 Java 비동기 수업과 기존 Findex의 `CompletableFuture + 전용 Executor` 경험에 연결한다.

---

## 2. JWT Stateless 인증

### 현재까지 정리한 핵심

JWT Stateless 방식에서도 현재 요청 동안 인가와 사용자 접근을 위해 `Authentication`과 `SecurityContext`는 필요할 수 있다.

차이는 요청 종료 뒤 서버 Session에 인증 상태를 보존하지 않는다는 점이다.

```text
요청
→ Bearer Access Token
→ 서명 / 만료 검증
→ Claim 확인
→ Authentication 구성
→ SecurityContext
→ 인가
→ 요청 종료
```

다음 요청에서는 기존 Session의 SecurityContext를 복원하는 대신 Token을 다시 검증해 인증 상태를 구성한다.

### JWT Signature

JWT Payload는 일반적으로 암호화된 비밀 내용이 아니라 Base64Url로 표현되어 내용을 확인할 수 있다.

Signature의 핵심 목적은 Payload를 숨기는 것이 아니라 토큰이 발급 이후 변조되었는지 검증하는 것이다.

따라서 비밀번호, 주민번호 등 노출되어서는 안 되는 민감정보를 Payload에 넣지 않는다.

---

## 3. Access Token 만료와 Refresh Token

Access Token을 짧게 유지하는 이유 중 하나는 탈취됐을 때 악용 가능한 시간을 줄이기 위해서다.

```text
Access Token
→ API 접근
→ 비교적 짧은 수명

Refresh Token
→ 새로운 Access Token 발급
→ 상대적으로 긴 수명
→ 일반 API 요청마다 보내지 않음
```

Refresh Token은 긴 수명을 가지므로 매 요청마다 전송하면 노출 기회가 증가한다.

### 서버 상태와 Stateless의 트레이드오프

JWT를 쓴다고 해서 인증 시스템 전체가 무조건 아무 상태도 관리하지 않는 것은 아니다.

예를 들어 다음 요구사항을 만족하려면 서버 상태를 일부 관리할 수 있다.

- Refresh Token 유효성 관리
- 강제 로그아웃
- Token Reuse 탐지
- Access Token Blocklist

Redis Blocklist를 매 요청 확인하면 토큰 폐기는 쉬워지지만 Redis 네트워크 조회와 저장소 의존성이 추가되어 Stateless의 장점 일부가 줄어든다.

서비스에 즉시 Access Token 폐기 요구가 낮고 Access Token 만료가 충분히 짧다면, Blocklist 없이 최대 만료 시간만큼의 지연을 허용하는 정책도 비교할 수 있다.

---

## 4. Refresh Token 저장

Refresh Token 원문을 DB/Redis에 영구 보관하면 저장소 유출 시 해당 토큰을 그대로 사용할 위험이 있다.

서버가 Refresh Token 원문을 다시 복원할 필요가 없다면 해시값을 저장하고 요청으로 들어온 토큰을 동일한 방식으로 해시해 비교하는 방법을 고려할 수 있다.

서버가 충분한 엔트로피의 긴 Random Token을 만든다면 비밀번호처럼 사용자가 추측하기 쉬운 값과 특성이 다르므로 반드시 BCrypt 같은 느린 비밀번호 해시와 동일한 방식으로 저장해야 하는 것은 아니다.

---

## 5. Refresh Token Rotation과 Reuse Detection

```text
Refresh Token A
→ 재발급 성공
→ A 폐기
→ Refresh Token B 발급
```

이미 폐기된 A가 나중에 다시 들어오면 정상적인 클라이언트 재사용인지 탈취된 토큰인지 판단해야 한다.

단순히 다른 IP라는 이유만으로 공격으로 확정하면 안 된다. 모바일 네트워크, NAT, 프록시 등으로 정상 사용자도 IP가 바뀔 수 있기 때문이다.

보수적인 정책에서는 이미 사용된 토큰의 재사용을 탐지하면 해당 Token Family를 폐기하고 재로그인을 요구하는 방식을 고려할 수 있다.

---

## 6. Refresh Token Rotation의 동시성

동일한 Refresh Token A로 두 요청이 거의 동시에 들어오면 다음 경쟁 상태가 생길 수 있다.

```text
요청 1: A = ACTIVE 확인
요청 2: A = ACTIVE 확인

요청 1: B 발급
요청 2: C 발급
```

A는 한 번만 사용되어야 하므로 두 요청이 모두 성공하면 안 된다.

### 후보 1: 비관적 락

```text
SELECT ... FOR UPDATE
→ A Row 잠금
→ ACTIVE 확인
→ USED 변경
→ 새 Token 저장
→ Commit
```

확실하게 직렬화할 수 있지만 Lock 대기와 트랜잭션 유지 시간이 생긴다.

### 후보 2: 조건부 UPDATE

```sql
UPDATE refresh_token
SET status = 'USED'
WHERE id = ?
  AND status = 'ACTIVE';
```

변경 row가 1이면 성공, 0이면 이미 소비된 Token으로 판단할 수 있다.

이 문제의 핵심은 전형적인 단순 Lost Update라기보다 **하나의 Token이 두 번 소비되는 Check-then-act 경쟁 상태**다.

조건부 UPDATE 역시 DB 내부 Lock과 동시성 제어가 사라지는 것은 아니지만, 애플리케이션의 별도 `SELECT FOR UPDATE → 상태 확인 → UPDATE` 흐름을 줄일 수 있다.

---

## 7. Rotation과 Transaction

기존 A를 `USED`로 만들고 새로운 B를 저장하는 것은 하나의 논리적 작업이다.

```text
Transaction
→ A: ACTIVE → USED
→ B 저장
→ 둘 다 성공: COMMIT
→ B 저장 실패: ROLLBACK
```

B 저장이 실패했는데 A만 USED로 남으면 사용자가 재발급할 방법을 잃을 수 있으므로 두 DB 작업의 원자성을 고려해야 한다.

---

## 8. 응답 유실과 멱등성

DB에서는 A가 USED, B가 ACTIVE로 정상 Commit됐지만 네트워크 오류로 클라이언트가 B를 받지 못할 수 있다.

이때 A를 다시 요청했다고 즉시 공격으로 판단하면 정상적인 네트워크 재시도를 오탐할 수 있다.

후보:

- 짧은 Grace Period
- Idempotency Key
- 재발급 결과를 아주 짧은 TTL로 공유 Cache에 보관

DB에는 B의 Hash만 보관한다면 Hash에서 B 원문을 다시 복원할 수 없다. 동일 B를 다시 전달해야 하는 요구사항이 있다면 짧은 시간 동안 재시도 결과를 안전하게 보관하는 별도 설계를 검토해야 한다.

Redis 같은 공유 Cache를 사용할 경우 다중 인스턴스에서도 재시도 결과를 공유할 수 있지만, 민감한 Token 원문 저장 위험, Redis 장애 의존성, TTL 정책을 함께 고려해야 한다.

---

## 9. 사용자 상태 변경과 JWT

JWT 발급 뒤 DB에서 사용자가 탈퇴하거나 `enabled=false`로 바뀌어도, 서버가 매 요청 Token 정보만 신뢰한다면 기존 Access Token 만료 전까지 상태 변경이 즉시 반영되지 않을 수 있다.

선택지는 서비스 요구사항에 따라 달라진다.

- 짧은 Access Token 만료
- Refresh 시점에 사용자 상태 재확인
- 중요한 요청에서 사용자 상태 추가 조회
- 즉시 폐기가 필요하다면 Blocklist 등 별도 상태 관리

즉 JWT 사용 여부만으로 정답을 정하지 않고 **즉시 반영 요구와 운영 복잡성의 트레이드오프**를 비교한다.

---

## 10. 세션 권한 변경과 실행 중 요청

세션 사용자가 로그인한 뒤 DB의 Role이 바뀌어도 기존 Session의 Authentication 권한이 자동으로 최신화된다고 가정하면 안 된다.

권한 회수를 즉시 반영하려면 다음 방법을 비교할 수 있다.

- 기존 활성 Session 만료 후 재로그인
- 중요 작업 직전에 최신 권한 재확인
- 권한 Version 관리

현재 프로젝트의 `UserRoleManager`는 Role 변경 뒤 `SessionRegistry`에서 대상 사용자의 활성 Session을 찾아 `expireNow()`로 만료시킨다. 이는 다음 요청부터 새 인증을 요구하게 하는 전략이다.

다만 이미 `@PreAuthorize`를 통과해 실행 중인 요청은 권한을 회수했다고 자동으로 중간 취소되지 않는다. 실행 중 요청까지 보호해야 하는 중요 작업은 실제 변경 직전에 DB 상태를 재확인하거나, 권한 조건을 포함한 조건부 UPDATE, 필요한 범위의 비관적 락 등을 업무 규칙에 맞게 검토한다.

이 문제는 검사 시점과 사용 시점이 달라지는 TOCTOU 문제이며, 인증·인가뿐 아니라 동시성, 데이터 정합성, 트랜잭션 정책과 연결된다.

---

## 11. 동일 세션의 동시 요청과 객체 공유

같은 HTTP Session에서 동시에 처리되는 요청은 Session에 저장된 같은 `SecurityContext` 참조를 공유할 수 있다.

ThreadLocal은 각 Thread가 Context에 접근하는 위치를 분리하지만, 그 안에 넣는 Context 객체 자체가 언제나 새 객체라는 뜻은 아니다.

따라서 한 요청에서 기존 `Authentication`이나 `SecurityContext`를 직접 변경하면 같은 세션의 다른 동시 요청에 영향을 줄 수 있다. 요청별로 임시 인증 변경이 필요하다면 기존 공유 객체를 제자리에서 수정하지 말고 새 `SecurityContext`를 만들어 요청 범위에 설정하는 방식을 검토한다.

---

## 12. CSRF·CORS·XSS의 관계

### CSRF

세션 쿠키는 브라우저가 대상 서버 요청에 자동으로 첨부한다. 공격자는 피해자의 비밀번호나 Session ID를 몰라도 피해자의 브라우저가 인증 쿠키를 보내게 만들 수 있으므로, 상태 변경 요청에는 쿠키만으로는 알 수 없는 CSRF Token을 추가로 검증한다.

현재 프로젝트는 `CookieCsrfTokenRepository.withHttpOnlyFalse()`를 사용한다. 클라이언트가 `XSRF-TOKEN` 쿠키 값을 읽어 `X-XSRF-TOKEN` 요청 Header로 보내는 방식이다. Token이 없거나 서버가 기대한 값과 다르면 `CsrfFilter`에서 403으로 거부되며, 로그인 요청이라면 `UsernamePasswordAuthenticationFilter`까지 도달하지 않는다.

쿠키 값은 클라이언트가 바꿀 수 있으므로, 쿠키에 값이 존재한다는 사실만으로 검증이 끝나는 것이 아니다. 현재의 Cookie 기반 Repository는 CSRF Cookie에서 불러온 기대값과 Header의 요청값을 비교한다. Session에 기대값을 저장하는 `HttpSessionCsrfTokenRepository`와 저장 위치를 혼동하지 않는다.

### CORS

CORS는 브라우저에서 다른 Origin의 Script가 요청과 응답을 다루는 범위를 제한한다. 특히 응답 읽기와 Preflight 승인에 관여한다.

CORS가 거부됐다는 사실만으로 CSRF 공격이 막혔다고 판단하면 안 된다. 일부 Cross-Origin 요청은 전송될 수 있고 브라우저가 응답만 Script에 공개하지 않을 수 있기 때문이다. CSRF 방어는 CSRF Token과 SameSite 등 별도 수단으로 설계한다.

### XSS

XSS로 신뢰하는 Origin에서 공격자 Script가 실행되면 그 Script는 페이지의 CSRF Token을 읽거나 정상 코드처럼 API를 호출할 수 있다. 현재처럼 `XSRF-TOKEN`을 JavaScript가 읽어야 해서 HttpOnly를 끈 구조에서는 XSS가 Token까지 읽을 수 있다.

Session Cookie에 HttpOnly를 설정하면 JavaScript가 Cookie 값을 직접 훔치는 것은 어렵게 하지만, 브라우저는 같은 사이트 요청에 해당 Cookie를 자동 첨부할 수 있다. 따라서 HttpOnly만으로 XSS가 피해자 권한의 요청을 실행하는 것까지 막지는 못한다.

### Cookie 속성 구분

| 속성 | 핵심 역할 |
|---|---|
| `HttpOnly` | JavaScript의 Cookie 값 직접 읽기를 제한한다. |
| `Secure` | HTTPS 연결에서만 Cookie를 전송하도록 한다. |
| `SameSite` | Cross-Site 상황에서 Cookie 전송 범위를 제한한다. `Strict`, `Lax`, `None`의 동작이 다르다. |

`SameSite`의 Site 개념은 CORS의 Origin과 같지 않다. 세 속성은 서로 대체 관계가 아니며 HTTPS, CSP, 출력 인코딩, 입력 처리, CSRF Token 등과 함께 방어 계층을 구성한다.

---

## 13. 다중 인스턴스의 Session 관리

### Sticky Session

Sticky Session은 특정 사용자의 후속 요청을 같은 애플리케이션 인스턴스로 라우팅하는 방식이다. “고유한 종류의 세션”을 새로 만드는 것이 아니다.

서버 로컬 메모리에 Session을 둘 수 있다는 장점이 있지만, 해당 인스턴스 장애, 배포, 증설·축소, 라우팅 변경 시 Session 연속성이 깨질 수 있고 특정 서버에 부하가 몰릴 수 있다.

### Spring Session과 Redis

Spring Session은 애플리케이션이 사용하는 `HttpSession` 저장소를 Redis 같은 공유 저장소로 교체할 수 있게 한다.

```text
브라우저
→ Session ID Cookie

애플리케이션 A / B
→ 같은 Session ID로 공유 저장소 조회

Redis
→ Session 속성
→ SecurityContext
```

브라우저에는 기존처럼 Session ID만 두고, SecurityContext와 인증 정보는 서버 측 Session 속성으로 유지한다. 어느 애플리케이션 인스턴스가 요청을 받아도 같은 Redis Session을 조회할 수 있다.

대신 Redis 가용성, 네트워크 비용, Session TTL, 직렬화 호환성, 저장 데이터 보호, 장애 시 로그인 영향까지 운영 범위에 포함된다.

---

## 14. Provider와 Filter 순서 기록 위치

여러 `AuthenticationProvider`의 `supports`, `null`, 성공 결과, 예외 처리 순서는 [01-authentication-flow.md](./01-authentication-flow.md)에 정리했다.

현재 프로젝트에서 활성화된 Security Filter의 전체 순서와 `ExceptionTranslationFilter`가 뒤쪽 Filter의 예외를 처리하는 구조는 [04-security-filter-chain.md](./04-security-filter-chain.md)에 정리했다.

이 두 주제도 구두 학습에서 시간을 들여 확인했으므로 문서에 남기되, 암기보다는 책임과 분기 기준을 설명하는 데 초점을 둔다.

---

## 15. 현재 단계의 완료 기준

위 Deep Dive 문제를 모두 현재 미션에 구현하는 것이 완료 조건은 아니다.

먼저 다음 핵심 흐름을 자료 없이 설명하고 현재 프로젝트의 테스트로 연결할 수 있어야 한다.

```text
Form Login 최초 인증
→ AuthenticationManager / Provider
→ UserDetailsService / PasswordEncoder
→ SecurityContext / Session
→ 다음 요청에서 인증 상태 복원
→ URL / Method Authorization
→ 401 / 403
```

Deep Dive 내용은 학습 기록에서 제외하지 않는다. 다음 미션이나 실제 문제가 생기면 해당 절을 출발점으로 삼아 재현하고 검증한다.
