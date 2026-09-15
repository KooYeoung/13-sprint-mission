# Security Filter Chain

## 1. 왜 Filter에서 동작하는가

Spring Security의 인증·인가는 특정 Controller 하나의 비즈니스 로직이 아니라 여러 HTTP 요청에 공통적으로 적용되는 보안 관심사다.

따라서 Controller에 도달하기 전 Filter 영역에서 공통으로 처리할 수 있다.

```text
HTTP Request
→ Servlet Filter
→ Spring Security Filter Chain
→ DispatcherServlet
→ Controller
```

각 Controller에서 매번 "로그인했는가?", "권한이 있는가?"를 직접 반복해서 확인하는 구조를 피할 수 있다.

---

## 2. 로그인 요청과 일반 요청은 다르다

HTTP 요청이 들어온다고 해서 모든 요청에서 username/password 인증을 새로 수행하는 것은 아니다.

### Form Login 요청

```text
POST /api/auth/login
→ 로그인 인증 Filter가 처리
→ 인증 전 Authentication
→ AuthenticationManager
→ Provider
→ 실제 인증
```

### 로그인 이후 일반 요청

```text
API 요청
→ 기존 Session에서 SecurityContext 복원
→ 기존 Authentication 사용
→ 인가
→ Controller
```

### 익명 사용자의 공개 요청

```text
GET /public
→ 정상 로그인 Authentication 없음
→ 익명 인증 기능이 활성화되어 있다면 AnonymousAuthenticationToken 사용 가능
→ permitAll
→ Controller
```

---

## 3. 주요 Filter 관계

모든 Security Filter 이름과 정확한 전체 순서를 암기하는 것보다 이번 학습에서는 다음 관계를 설명할 수 있는 것을 목표로 한다.

```text
SecurityContext 로드/설정
        ↓
필요한 Authentication Filter
        ↓
AnonymousAuthenticationFilter
        ↓
ExceptionTranslationFilter
        ↓
AuthorizationFilter
        ↓
Controller
```

### AnonymousAuthenticationFilter

앞선 인증 처리 이후에도 정상적인 Authentication이 없다면 익명 사용자 객체를 설정할 수 있다.

다른 인증 Filter가 이 Filter에게 인증을 "위임"하는 구조로 이해하지 않는다.

### AuthorizationFilter

현재 `Authentication`과 요청의 인가 규칙을 바탕으로 접근 가능 여부를 판단한다.

### ExceptionTranslationFilter

뒤쪽 인가 처리 등에서 발생한 Spring Security 예외를 받아 `AuthenticationEntryPoint` 또는 `AccessDeniedHandler`와 연결한다.

즉 `ExceptionTranslationFilter`가 `AuthorizationFilter`에게 권한 판단을 위임한다기보다, Filter Chain 뒤에서 발생한 예외를 웹 응답으로 변환할 수 있도록 감싸는 관계로 이해한다.

---

## 4. permitAll은 Filter Chain 제외가 아니다

현재 `SecurityConfig`는 로그인, 로그아웃, 회원가입, 정적 리소스 등에 `permitAll()`을 설정한다.

```text
permitAll
→ Security Filter Chain은 통과
→ 인가 단계에서 인증 여부와 관계없이 접근 허용
```

반대로 어떤 요청이 아예 어떤 `SecurityFilterChain`에도 매칭되지 않으면 Spring Security의 보호를 받지 않을 수 있다.

공개 API라고 해서 곧바로 Security Filter Chain 전체에서 제외하는 것과 `permitAll()`은 같은 의미가 아니다.

---

## 5. requestMatchers

현재 프로젝트처럼 하나의 `SecurityFilterChain` 안에서 `requestMatchers`를 사용하면 요청 경로별 인가 정책을 나눌 수 있다.

```java
.requestMatchers("/api/auth/login").permitAll()
.requestMatchers("/actuator", "/actuator/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

개념적으로는 다음 역할이다.

```text
이미 선택된 SecurityFilterChain 안에서
→ 이 요청에 어떤 인가 규칙을 적용할 것인가?
```

더 구체적인 규칙을 앞에 두고 넓은 규칙을 뒤에 두어 의도하지 않은 선행 매칭을 피한다.

---

## 6. securityMatcher와 여러 SecurityFilterChain

현재 프로젝트는 하나의 `SecurityFilterChain`을 사용하지만, 여러 체인을 구성한다면 `securityMatcher`는 **해당 SecurityFilterChain 자체가 어떤 요청을 담당할지** 결정한다.

```text
securityMatcher
→ 어떤 SecurityFilterChain을 사용할 것인가?

requestMatchers
→ 선택된 SecurityFilterChain 안에서 어떤 인가 규칙을 사용할 것인가?
```

여러 `SecurityFilterChain`이 있다면 `@Order` 순서대로 확인하고 처음 매칭된 체인 하나를 사용한다.

예를 들어:

```text
@Order(1) /api/**
@Order(2) /**
```

`/api/users`는 첫 번째 체인에 매칭되므로 두 번째 체인까지 모두 거치는 것이 아니다.

기본 체인이 없이 어떤 `securityMatcher`에도 해당하지 않는 URL이 생기면 Spring Security 보호에서 빠질 수 있으므로 구성 시 주의해야 한다.

---

## 7. deny-by-default

보안 기본값은 필요한 것만 명시적으로 허용하는 방식이 안전하다.

```text
새 API 추가
→ 별도 인가 규칙 누락

기본 permitAll
→ 의도치 않게 외부 공개될 수 있음

기본 denyAll
→ 명시적으로 허용하기 전까지 차단
```

현재 프로젝트는 공개 경로를 먼저 지정하고 나머지 요청을 `.anyRequest().authenticated()`로 보호한다.

---

## 8. 현재 프로젝트의 흐름

현재 `SecurityConfig`를 단순화하면 다음과 같다.

```text
공개
- /api/auth/csrf-token
- /api/auth/login
- /api/auth/logout
- POST /api/users
- 정적 리소스 / Swagger

ADMIN 필요
- /actuator/**
- /api/auth/role

나머지
- authenticated()
```

추가로 Method Security가 활성화되어 있으므로 일부 세부 권한은 Controller 진입 이후 Service 메서드 수준에서도 검사할 수 있다.
