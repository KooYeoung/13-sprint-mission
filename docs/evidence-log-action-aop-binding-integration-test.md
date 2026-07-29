# LogAction AOP 바인딩 오류 통합 테스트 증거

## 목적

통합 테스트 작성 중 `@LogAction`이 붙은 Service 메서드를 실제 API 요청 경로로 호출했을 때 아래 예외가 발생했다.

```text
java.lang.IllegalStateException: Required to bind 2 arguments, but only bound 1 (JoinPointMatch was NOT bound in invocation)
```

이 문서는 해당 문제가 왜 발생했는지, 왜 Controller 슬라이스 테스트에서는 드러나지 않았는지, 그리고 왜 `MethodSignature` 기반으로 `LogAction`을 직접 해석하도록 수정했는지를 기록한다.

결론부터 정리하면, 원인은 "프록시를 거치지 않아서"가 아니다.

오히려 실제 Spring AOP 프록시를 거쳐 `BusinessActionLoggingAspect`가 실행됐고, 그 과정에서 `@annotation(logAction)` pointcut의 어노테이션 변수를 advice 메서드 파라미터로 바인딩하지 못해 실패했다.

---

## 재현된 흐름

통합 테스트는 다음처럼 실제 HTTP 요청부터 DB 저장까지 전체 Spring Bean 경로를 실행한다.

```text
MockMvc
-> Controller 실제 Bean
-> Service 실제 Bean 또는 AOP Proxy
-> @LogAction pointcut 매칭
-> BusinessActionLoggingAspect 실행
-> advice 파라미터 바인딩 실패
-> Controller 응답 500
```

따라서 이 문제는 AOP가 실행되지 않아서 생긴 문제가 아니다.

만약 프록시를 거치지 않았다면 `BusinessActionLoggingAspect.logServiceAction(...)` 자체가 실행되지 않았을 가능성이 크고, 위 바인딩 예외도 발생하지 않았을 것이다.

---

## 실패 로그

통합 테스트 실행 명령은 다음과 같다.

```powershell
.\gradlew.bat test --tests "com.sprint.mission.discodeit.integration.DiscodeitApiIntegrationTest"
```

초기 실패 당시 모든 API 흐름의 첫 사용자 생성 요청이 `201 Created`가 아니라 `500 Internal Server Error`를 반환했다.

테스트 리포트에서 확인한 핵심 예외는 다음과 같다.

```text
java.lang.IllegalStateException: Required to bind 2 arguments, but only bound 1 (JoinPointMatch was NOT bound in invocation)
Status = 500
Body = {"status":500,"exceptionType":"IllegalStateException","code":"INTERNAL_SERVER_ERROR","message":"서버 내부 오류가 발생했습니다.",...}
```

이 예외는 사용자 생성 Service 로직 자체의 검증 실패가 아니라, Service 호출 전에 적용되는 AOP advice 실행 단계에서 발생한 것이다.

---

## 기존 Aspect 구현

문제가 된 기존 구현은 다음 형태였다.

```java
@Around("@annotation(logAction)")
public Object logServiceAction(ProceedingJoinPoint joinPoint, LogAction logAction) throws Throwable {
    log.info("{} 시작", logAction.value());
    Object result = joinPoint.proceed();
    logResult(logAction, joinPoint, result);
    return result;
}
```

이 방식은 pointcut의 `logAction` 변수를 advice 메서드의 `LogAction logAction` 파라미터에 바인딩하는 방식이다.

즉 Spring AOP가 아래 두 조건을 모두 만족해야 정상 동작한다.

```text
1. @annotation(logAction) pointcut이 대상 메서드의 @LogAction을 찾는다.
2. pointcut에서 찾은 logAction 변수를 advice 메서드 두 번째 파라미터에 연결한다.
```

실패 로그의 `Required to bind 2 arguments, but only bound 1`는 이 바인딩 단계가 깨졌다는 의미다.

---

## 왜 슬라이스 테스트에서는 드러나지 않았는가

Controller 슬라이스 테스트에서는 일반적으로 Controller만 실제 Bean으로 올리고, Service는 `@MockBean` 또는 Mockito Mock으로 대체한다.

따라서 요청 흐름은 다음에 가깝다.

```text
MockMvc
-> Controller 실제 Bean
-> Service Mock
```

이 경우 Service 실제 구현체가 실행되지 않고, Service에 적용될 AOP 프록시도 통합 경로처럼 검증되지 않는다.

그래서 Controller 슬라이스 테스트는 다음 항목을 검증하기에 적합하다.

```text
- URL 매핑
- HTTP method
- request part, request body, query parameter 바인딩
- Bean Validation
- 응답 status와 JSON 직렬화
- Controller가 Service에 넘기는 인자
```

반면 다음 항목은 Controller 슬라이스 테스트만으로 검증하기 어렵다.

```text
- 실제 Service 구현 실행
- Service에 적용되는 AOP advice
- 실제 Repository 호출
- 실제 DB 저장과 재조회
- Mapper, Storage 등 여러 Bean의 조합
```

이번 오류는 두 번째 그룹에 속하므로 통합 테스트에서 처음 드러난 것이 자연스럽다.

---

## 왜 MethodSignature 방식으로 수정했는가

수정 후 구현은 advice 메서드에 `LogAction`을 직접 파라미터로 받지 않는다.

```java
@Around("@annotation(com.sprint.mission.discodeit.aspect.LogAction)")
public Object logServiceAction(ProceedingJoinPoint joinPoint) throws Throwable {
    LogAction logAction = resolveLogAction(joinPoint);

    log.info("{} 시작", logAction.value());
    Object result = joinPoint.proceed();
    logResult(logAction, joinPoint, result);
    return result;
}
```

pointcut은 여전히 `@LogAction`이 붙은 메서드에만 Aspect를 적용한다.

다만 `logAction`이라는 pointcut 변수를 advice 파라미터로 바인딩하지 않고, `MethodSignature`를 통해 실행 대상 메서드에서 직접 어노테이션을 조회한다.

```java
private LogAction resolveLogAction(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    LogAction logAction = method.getAnnotation(LogAction.class);
    if (logAction != null) {
        return logAction;
    }

    Method targetMethod = joinPoint.getTarget()
            .getClass()
            .getMethod(method.getName(), method.getParameterTypes());
    return targetMethod.getAnnotation(LogAction.class);
}
```

이 방식의 장점은 다음과 같다.

```text
1. advice 파라미터 바인딩 규칙에 의존하지 않는다.
2. 인터페이스 기반 프록시와 구현체 메서드 어노테이션 차이를 완화할 수 있다.
3. pointcut은 적용 대상만 판단하고, 어노테이션 값 해석은 Java reflection으로 명확히 처리한다.
```

---

## 검증 결과

수정 후 통합 테스트만 다시 실행했다.

```powershell
.\gradlew.bat test --tests "com.sprint.mission.discodeit.integration.DiscodeitApiIntegrationTest"
```

결과는 성공이다.

```text
BUILD SUCCESSFUL
```

이후 전체 테스트도 실행했다.

```powershell
.\gradlew.bat test
```

결과도 성공이다.

```text
BUILD SUCCESSFUL
```

---

## 정리

이번 문제는 프록시가 적용되지 않은 문제가 아니다.

통합 테스트에서 실제 Controller, Service, AOP, Repository, DB 흐름이 실행되면서 `@LogAction` Aspect가 실제로 동작했고, 그 과정에서 `@annotation(logAction)`의 어노테이션 변수 바인딩이 실패한 것이다.

Controller 슬라이스 테스트에서는 Service가 Mock으로 대체되어 이 경로가 실행되지 않으므로 발견하기 어렵다.

따라서 이번 통합 테스트는 단순히 API 성공 흐름만 검증한 것이 아니라, 실제 운영 경로에 가까운 Spring Bean 조합에서 AOP 설정 오류까지 드러낸 evidence로 볼 수 있다.
