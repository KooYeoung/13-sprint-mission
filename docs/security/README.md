# Spring Security 학습 기록

이 디렉터리는 `정구영-sprint9`에서 실제로 적용한 Spring Security 코드와 구두 점검 과정에서 확인한 내용을 다시 설명할 수 있도록 정리한 문서다.

단순히 클래스와 메서드 이름을 외우는 것보다 다음 흐름을 이해하는 것을 목표로 한다.

```text
HTTP 요청
→ 인증이 필요한가?
→ 사용자는 누구인가?
→ 어떤 권한을 가지고 있는가?
→ 요청한 자원에 접근할 수 있는가?
→ 실패했다면 401인가 403인가?
```

## 문서 구성

| 문서 | 내용 |
| --- | --- |
| [01-authentication-flow.md](01-authentication-flow.md) | Form Login 최초 인증, AuthenticationManager, Provider, UserDetailsService, PasswordEncoder |
| [02-security-context-and-session.md](02-security-context-and-session.md) | SecurityContext, SecurityContextHolder, Session, JSESSIONID, ThreadLocal, 로그아웃 |
| [03-authorization-and-exception.md](03-authorization-and-exception.md) | 인증/인가, authorities, role, 401/403, AuthenticationEntryPoint, AccessDeniedHandler |
| [04-security-filter-chain.md](04-security-filter-chain.md) | SecurityFilterChain, permitAll, requestMatchers, securityMatcher, 주요 Filter 관계 |
| [05-security-testing.md](05-security-testing.md) | Security 테스트 범위, Mock 사용자와 실제 인증 테스트의 차이 |
| [06-deep-dive-notes.md](06-deep-dive-notes.md) | 비동기 SecurityContext, JWT/Refresh Token, 권한 변경 등 추후 심화할 주제 |

## 현재 프로젝트와 연결되는 코드

현재 프로젝트에서는 다음 코드가 이 문서의 기준이 된다.

- `src/main/java/com/sprint/mission/discodeit/config/SecurityConfig.java`
  - Form Login: `/api/auth/login`
  - Logout: `/api/auth/logout`
  - 공개 경로와 보호 경로 설정
  - `AuthenticationEntryPoint`, `AccessDeniedHandler`
  - Session 동시 접속 제한
  - Role Hierarchy
- `src/main/java/com/sprint/mission/discodeit/security/DiscodeitUserDetailsService.java`
  - username을 기준으로 사용자 조회
- `src/main/java/com/sprint/mission/discodeit/security/DiscodeitUserDetails.java`
  - 사용자 정보와 `ROLE_` 권한을 Spring Security 형식으로 제공
- `src/main/java/com/sprint/mission/discodeit/config/PasswordEncoderConfig.java`
  - `BCryptPasswordEncoder` 사용
- `src/test/java/com/sprint/mission/discodeit/security/SecurityAuthorizationIntegrationTest.java`
  - 미인증 401, 권한 부족 403, 관리자 접근, Role Hierarchy 검증

## 이번 학습에서 교정한 핵심 오해

### 인증 전 Authentication과 익명 Authentication은 다르다

Form Login을 시도할 때 만들어지는 인증 전 `Authentication`과, 로그인하지 않은 일반 사용자를 표현하는 `AnonymousAuthenticationToken`은 같은 개념이 아니다.

```text
로그인 요청
→ username/password를 담은 인증 전 Authentication

일반 익명 요청
→ 인증 정보가 없으면 AnonymousAuthenticationToken을 사용할 수 있음
```

### 세션 복원은 새로운 로그인이 아니다

한 번 로그인한 세션 사용자는 이후 요청마다 DB에서 사용자를 다시 조회하고 비밀번호를 다시 비교하지 않는다.

```text
최초 로그인
→ 실제 인증
→ SecurityContext 저장

이후 요청
→ JSESSIONID
→ 기존 Session 조회
→ SecurityContext 복원
→ 기존 Authentication 사용
```

### permitAll은 Security를 우회하는 설정이 아니다

`permitAll()`은 Security Filter Chain을 통과하되 해당 요청의 접근을 인증 여부와 관계없이 허용하는 인가 규칙이다.

### 401과 403은 다르다

```text
401 Unauthorized
→ 인증이 필요한데 정상적인 인증 상태가 없음

403 Forbidden
→ 인증은 되었지만 요청한 작업에 필요한 권한이 없음
```

## 이번 주 학습 범위에서 일부러 깊게 다루지 않는 내용

현재 `SecurityConfig`에는 CSRF, Remember Me, Session Registry, Role Hierarchy 같은 설정도 포함되어 있다. 이번 문서에서는 인증·인가 기본 흐름을 먼저 설명할 수 있는 수준으로 정리하고, 구현 내부 세부사항은 필요한 시점에 별도 Deep Dive로 확장한다.

JWT와 Java 비동기는 다음 학습 범위와 연결되므로 [06-deep-dive-notes.md](06-deep-dive-notes.md)에 현재까지 확인한 질문과 개념만 남긴다.
