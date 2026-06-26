# ErrorResponse, ProblemDetail, ResponseEntity 에러 응답 방식 정리

## PR Review Context

이번 PR에서는 전역 예외 처리를 위해 직접 만든 `ErrorResponse` DTO와 `ResponseEntity`를 사용했다.

```java
package com.sprint.mission.discodeit.exception;

import java.util.Map;

public record ErrorResponse(
        String message,
        Map<String, String> fields
) {
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null);
    }

    public static ErrorResponse of(String message, Map<String, String> fields) {
        return new ErrorResponse(message, fields);
    }
}
```

```java
@ExceptionHandler(CustomBadRequestException.class)
public ResponseEntity<ErrorResponse> badRequest(RuntimeException e) {
    log.warn(e.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of(e.getMessage()));
}
```

리뷰에서는 아래 내용을 확인해보라는 피드백을 받았다.

- `ResponseEntity` 없이 `ProblemDetail`만 사용하는 방식
- `ResponseEntity`와 `ProblemDetail`을 함께 사용하는 방식
- `ResponseEntity`와 커스텀 에러 응답 객체를 함께 사용하는 방식
- Spring에도 `org.springframework.web.ErrorResponse`라는 인터페이스가 있으므로, 직접 만든 `ErrorResponse` 이름과 혼동될 수 있음

---

## 현재 구현 방식

현재 구현은 다음 선택지 중 **3번 방식**에 해당한다.

```text
3 - ResponseEntity와 커스텀 객체 사용하기
```

즉, 상태 코드는 `ResponseEntity`로 지정하고, 응답 본문은 직접 만든 DTO인 `ErrorResponse`로 내려준다.

예상 응답 예시는 다음과 같다.

```json
{
  "message": "잘못된 요청입니다.",
  "fields": null
}
```

검증 실패 시에는 다음과 같은 형태로 응답한다.

```json
{
  "message": "요청 본문에 일부 필드가 유효하지 않습니다.",
  "fields": {
    "username": "사용자 이름은 필수입니다.",
    "email": "이메일 형식이 올바르지 않습니다."
  }
}
```

이 방식은 응답 구조를 직접 설계할 수 있다는 장점이 있다. 다만 Spring에서 제공하는 `org.springframework.web.ErrorResponse`와 이름이 동일하기 때문에 코드 가독성이나 유지보수 측면에서 혼동이 생길 수 있다.

---

## Spring의 ProblemDetail

`ProblemDetail`은 Spring Framework 6부터 제공되는 에러 응답 객체이다. RFC 9457 Problem Details 형식에 맞춰 오류 응답을 표현할 수 있다.

대표 필드는 다음과 같다.

| 필드 | 의미 |
| --- | --- |
| `type` | 에러 유형을 나타내는 URI |
| `title` | 에러 제목 |
| `status` | HTTP 상태 코드 |
| `detail` | 구체적인 에러 메시지 |
| `instance` | 에러가 발생한 요청 경로 |

예시 응답은 다음과 같다.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "잘못된 요청입니다.",
  "instance": "/api/users"
}
```

`ProblemDetail`은 표준 필드 외에 추가 필드를 넣을 수도 있다. 예를 들어 검증 실패 시 `fields`라는 추가 속성을 넣을 수 있다.

```java
problemDetail.setProperty("fields", fieldsMap);
```

Jackson을 사용하는 경우 `ProblemDetail`의 추가 속성은 JSON의 최상위 필드로 내려간다.

---

## 선택지 1: ProblemDetail만 사용하기

단순히 상태 코드와 에러 메시지만 내려주면 되는 경우에는 `ResponseEntity` 없이 `ProblemDetail`만 반환할 수 있다.

```java
@ExceptionHandler(CustomBadRequestException.class)
public ProblemDetail badRequest(CustomBadRequestException e) {
    log.warn(e.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getMessage()
    );

    problemDetail.setTitle("Bad Request");

    return problemDetail;
}
```

이 방식은 코드가 간단하고 Spring 표준 에러 응답 형식을 사용할 수 있다는 장점이 있다.

### 선택지 1 주의사항

#### 1. `ProblemDetail.status`가 실제 HTTP 상태 코드가 된다

`ProblemDetail`만 반환하는 경우에는 `ProblemDetail`의 `status` 값이 실제 HTTP 응답 상태 코드로 사용된다.

따라서 반드시 아래처럼 상태 코드를 가진 `ProblemDetail`을 만들어야 한다.

```java
ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "잘못된 요청입니다."
);
```

또는 직접 상태를 지정해야 한다.

```java
ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
problemDetail.setDetail("잘못된 요청입니다.");
```

상태 코드를 명확히 지정하지 않거나, 예외 상황과 맞지 않는 상태 코드를 넣으면 실제 응답 상태도 의도와 달라질 수 있다.

#### 2. `detail`에 내부 예외 메시지를 그대로 노출하지 않도록 주의한다

아래처럼 모든 예외에 대해 `e.getMessage()`를 그대로 내려주면 내부 구현 정보나 디버깅 메시지가 사용자에게 노출될 수 있다.

```java
ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        e.getMessage()
);
```

특히 `500 Internal Server Error` 계열에서는 사용자에게는 일반화된 메시지를 내려주고, 자세한 내용은 로그에 남기는 것이 좋다.

```java
@ExceptionHandler(Exception.class)
public ProblemDetail exception(Exception e) {
    log.error("Unhandled exception", e);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "알 수 없는 오류가 발생했습니다."
    );
    problemDetail.setTitle("Internal Server Error");

    return problemDetail;
}
```

#### 3. 추가 필드 이름은 프로젝트 내에서 일관되게 사용한다

`ProblemDetail`에는 `fields`, `errorCode`, `timestamp` 같은 추가 필드를 넣을 수 있다.

```java
problemDetail.setProperty("fields", fieldsMap);
problemDetail.setProperty("errorCode", "VALIDATION_FAILED");
```

다만 API마다 추가 필드 이름이 달라지면 프론트엔드에서 처리하기 어려워질 수 있다. 예를 들어 어떤 API는 `fields`, 다른 API는 `errors`, 또 다른 API는 `fieldErrors`를 사용하면 응답 규칙이 흔들린다.

따라서 검증 실패 응답은 `fields`, 비즈니스 에러 코드는 `errorCode`처럼 팀 내 규칙을 정해두는 것이 좋다.

---

## 선택지 2: ResponseEntity와 ProblemDetail 사용하기

`ProblemDetail`을 사용하면서도 상태 코드나 응답 헤더를 명시적으로 제어하고 싶다면 `ResponseEntity<ProblemDetail>`을 사용할 수 있다.

```java
@ExceptionHandler(CustomBadRequestException.class)
public ResponseEntity<ProblemDetail> badRequest(CustomBadRequestException e) {
    log.warn(e.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getMessage()
    );

    problemDetail.setTitle("Bad Request");

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problemDetail);
}
```

헤더를 추가해야 하는 경우에도 사용할 수 있다.

```java
return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header("X-Error-Code", "INVALID_REQUEST")
        .body(problemDetail);
```

단순한 에러 응답이라면 `ProblemDetail`만 반환해도 충분하지만, 헤더 제어나 응답 상태를 명확히 표현하고 싶다면 이 방식이 적절하다.

### 선택지 2 주의사항

#### 1. 실제 HTTP 상태 코드는 `ResponseEntity.status(...)`가 우선한다

`ResponseEntity<ProblemDetail>`을 사용할 때는 상태 코드가 두 군데에 존재할 수 있다.

첫 번째는 `ProblemDetail` 본문의 `status`이다.

```java
ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "잘못된 요청입니다."
);
```

두 번째는 `ResponseEntity`의 HTTP 상태 코드이다.

```java
return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(problemDetail);
```

이처럼 두 상태 코드가 다르면 실제 HTTP 응답 상태 코드는 `ResponseEntity.status(...)`에 지정한 값으로 내려간다. 반면 JSON 본문 안의 `status` 값은 `ProblemDetail`에 들어있는 값 그대로 내려간다.

예를 들어 아래 코드는 의도적으로 불일치한 예시이다.

```java
@ExceptionHandler(CustomBadRequestException.class)
public ResponseEntity<ProblemDetail> badRequest(CustomBadRequestException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "잘못된 요청입니다."
    );
    problemDetail.setTitle("Bad Request");

    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(problemDetail);
}
```

이 경우 응답은 개념적으로 아래처럼 불일치할 수 있다.

```http
HTTP/1.1 404 Not Found
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "잘못된 요청입니다.",
  "instance": "/api/users"
}
```

클라이언트 입장에서는 HTTP 상태 코드는 `404`인데, 본문에는 `400`이라고 적혀 있으므로 혼란이 생긴다. 따라서 `ResponseEntity`의 상태 코드와 `ProblemDetail.status`는 항상 일치시키는 것이 좋다.

#### 2. 같은 상태 값을 변수로 빼면 불일치를 줄일 수 있다

상태 코드 불일치를 막으려면 같은 `HttpStatus` 값을 재사용하는 방식이 좋다.

```java
@ExceptionHandler(CustomBadRequestException.class)
public ResponseEntity<ProblemDetail> badRequest(CustomBadRequestException e) {
    HttpStatus status = HttpStatus.BAD_REQUEST;

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            status,
            e.getMessage()
    );
    problemDetail.setTitle("Bad Request");

    return ResponseEntity
            .status(status)
            .body(problemDetail);
}
```

이렇게 하면 `ProblemDetail.status`와 `ResponseEntity.status(...)`가 서로 달라질 가능성을 줄일 수 있다.

#### 3. `ResponseEntity.of(problemDetail)`을 사용할 수도 있다

Spring은 `ProblemDetail`의 상태 값을 기반으로 `ResponseEntity`를 만드는 `ResponseEntity.of(ProblemDetail)` 메서드도 제공한다.

```java
@ExceptionHandler(CustomBadRequestException.class)
public ResponseEntity<?> badRequest(CustomBadRequestException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getMessage()
    );
    problemDetail.setTitle("Bad Request");

    return ResponseEntity
            .of(problemDetail)
            .header("X-Error-Code", "INVALID_REQUEST")
            .build();
}
```

이 방식은 `ProblemDetail.getStatus()`를 기준으로 `ResponseEntity`의 상태가 설정되므로 상태 코드 불일치를 줄이는 데 도움이 된다.

#### 4. 헤더 제어가 필요 없다면 `ProblemDetail`만 반환하는 편이 더 단순하다

`ResponseEntity<ProblemDetail>`은 헤더를 추가하거나 응답 상태를 명시적으로 제어해야 할 때 유용하다. 하지만 단순히 표준 에러 응답만 내려주면 되는 경우에는 아래처럼 `ProblemDetail`만 반환하는 코드가 더 간단하다.

```java
@ExceptionHandler(CustomBadRequestException.class)
public ProblemDetail badRequest(CustomBadRequestException e) {
    return ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getMessage()
    );
}
```

---

## 선택지 3: ResponseEntity와 커스텀 객체 사용하기

현재 코드가 이 방식이다.

```java
@ExceptionHandler(CustomNotFoundException.class)
public ResponseEntity<ErrorResponse> notFound(RuntimeException e) {
    log.warn(e.getMessage());

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(e.getMessage()));
}
```

이 방식은 프로젝트에서 원하는 응답 구조를 자유롭게 만들 수 있다는 장점이 있다. 하지만 Spring 표준 응답 형식과는 다르므로, 팀 내에서 에러 응답 규칙을 별도로 정의해야 한다.

또한 직접 만든 클래스 이름이 Spring의 `ErrorResponse`와 겹치지 않도록 이름을 변경하는 것이 좋다.

```java
public record ApiErrorResponse(
        String message,
        Map<String, String> fields
) {
    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse(message, null);
    }

    public static ApiErrorResponse of(String message, Map<String, String> fields) {
        return new ApiErrorResponse(message, fields);
    }
}
```

### 선택지 3 주의사항

#### 1. Spring의 `ErrorResponse`와 이름이 겹치지 않게 한다

현재 만든 클래스 이름은 다음과 같다.

```java
com.sprint.mission.discodeit.exception.ErrorResponse
```

하지만 Spring에도 다음 인터페이스가 있다.

```java
org.springframework.web.ErrorResponse
```

둘은 전혀 다른 객체이다. 직접 만든 `ErrorResponse`는 단순 응답 DTO이고, Spring의 `ErrorResponse`는 HTTP 상태 코드, 헤더, `ProblemDetail` body를 포함하는 에러 응답 인터페이스이다.

따라서 커스텀 DTO 방식을 유지한다면 이름을 아래처럼 바꾸는 것이 더 명확하다.

```text
ErrorResponse -> ApiErrorResponse
```

#### 2. 커스텀 응답 구조는 프론트엔드와 약속이 필요하다

커스텀 DTO를 사용하면 응답 구조를 자유롭게 만들 수 있다. 예를 들어 현재 구조는 `message`, `fields`를 사용한다.

```json
{
  "message": "요청 본문에 일부 필드가 유효하지 않습니다.",
  "fields": {
    "username": "사용자 이름은 필수입니다."
  }
}
```

하지만 Spring 표준인 `ProblemDetail`은 `detail`, `title`, `status`를 사용한다.

```json
{
  "title": "Validation Failed",
  "status": 400,
  "detail": "요청 본문에 일부 필드가 유효하지 않습니다.",
  "fields": {
    "username": "사용자 이름은 필수입니다."
  }
}
```

따라서 커스텀 DTO를 유지할 경우 프론트엔드와 에러 응답 구조를 명확히 약속해야 한다.

#### 3. HTTP 상태 코드와 body의 의미가 어긋나지 않게 한다

커스텀 DTO에는 보통 `status` 필드가 없기 때문에 실제 상태 코드는 `ResponseEntity.status(...)`만 보면 된다.

```java
return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiErrorResponse.of("사용자를 찾을 수 없습니다."));
```

하지만 커스텀 DTO에 `status` 필드를 추가한다면 `ResponseEntity.status(...)`와 DTO 내부의 `status` 값이 서로 다르지 않게 관리해야 한다.

```java
public record ApiErrorResponse(
        int status,
        String message,
        Map<String, String> fields
) {
}
```

이 경우 아래처럼 불일치가 생기지 않게 주의해야 한다.

```java
return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiErrorResponse(400, "사용자를 찾을 수 없습니다.", null));
```

위 코드는 HTTP 상태는 `404`인데 body의 `status`는 `400`이므로 좋지 않다.

#### 4. `Exception.class` 처리에서는 내부 메시지 노출을 피한다

커스텀 DTO 방식에서도 알 수 없는 예외에 대해 `e.getMessage()`를 그대로 내려주는 것은 피하는 것이 좋다.

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiErrorResponse> exception(Exception e) {
    log.error("Unhandled exception", e);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of("알 수 없는 오류가 발생했습니다."));
}
```

---

## Spring의 ErrorResponse

Spring에도 `org.springframework.web.ErrorResponse`라는 인터페이스가 있다.

직접 만든 객체:

```java
com.sprint.mission.discodeit.exception.ErrorResponse
```

Spring 제공 객체:

```java
org.springframework.web.ErrorResponse
```

Spring의 `ErrorResponse`는 단순 DTO가 아니라 다음 정보를 포함하는 에러 응답 인터페이스이다.

- HTTP status
- HTTP headers
- `ProblemDetail` body

`ErrorResponseException`은 이 인터페이스의 기본 구현체이며, 예외이면서 동시에 에러 응답 정보를 가진 객체로 사용할 수 있다.

예시는 다음과 같다.

```java
throw new ErrorResponseException(
        HttpStatus.BAD_REQUEST,
        ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "잘못된 요청입니다."
        ),
        null
);
```

다만 현재 프로젝트 단계에서는 `ErrorResponseException`까지 바로 적용하기보다는, 먼저 `ProblemDetail`을 직접 반환하는 방식부터 이해하는 것이 더 적절하다고 판단했다.

---

## Validation 예외를 ProblemDetail로 변경한 예시

기존 검증 예외 처리 코드는 커스텀 `ErrorResponse`를 사용했다.

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
    Map<String, String> fieldsMap = new HashMap<>();

    e.getBindingResult()
            .getFieldErrors()
            .forEach(field -> fieldsMap.put(field.getField(), field.getDefaultMessage()));

    log.warn("validation errors={}", fieldsMap);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.of("요청 본문에 일부 필드가 유효하지 않습니다.", fieldsMap));
}
```

`ProblemDetail`을 사용하면 다음과 같이 작성할 수 있다.

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
    Map<String, String> fieldsMap = new HashMap<>();

    e.getBindingResult()
            .getFieldErrors()
            .forEach(field ->
                    fieldsMap.put(field.getField(), field.getDefaultMessage())
            );

    log.warn("validation errors={}", fieldsMap);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "요청 본문에 일부 필드가 유효하지 않습니다."
    );

    problemDetail.setTitle("Validation Failed");
    problemDetail.setProperty("fields", fieldsMap);

    return problemDetail;
}
```

예상 응답은 다음과 같다.

```json
{
  "type": "about:blank",
  "title": "Validation Failed",
  "status": 400,
  "detail": "요청 본문에 일부 필드가 유효하지 않습니다.",
  "instance": "/api/users",
  "fields": {
    "username": "사용자 이름은 필수입니다.",
    "email": "이메일 형식이 올바르지 않습니다."
  }
}
```

---

## 선택지별 비교 정리

| 방식 | 특징 | 장점 | 주의사항 |
| --- | --- | --- | --- |
| `ProblemDetail`만 사용 | Spring 표준 에러 응답 객체만 반환 | 코드가 간단하고 표준 형식 사용 가능 | `ProblemDetail.status`가 실제 HTTP 상태 코드가 되므로 상태 설정을 정확히 해야 함 |
| `ResponseEntity<ProblemDetail>` 사용 | 상태 코드, 헤더, ProblemDetail body를 함께 제어 | 응답 헤더와 상태 코드 제어가 명확함 | `ResponseEntity.status(...)`와 `ProblemDetail.status`가 불일치하지 않게 해야 함 |
| `ResponseEntity<커스텀 DTO>` 사용 | 직접 만든 응답 구조 사용 | 프로젝트 요구사항에 맞게 자유롭게 설계 가능 | Spring 표준과 다르고, 직접 만든 `ErrorResponse` 이름이 Spring의 `ErrorResponse`와 충돌 가능 |

---

## 상태 코드 기준 정리

| 반환 방식 | 실제 HTTP 상태 코드 기준 | body 내부 status |
| --- | --- | --- |
| `ProblemDetail` 반환 | `ProblemDetail.status` | `ProblemDetail.status` |
| `ResponseEntity<ProblemDetail>` 반환 | `ResponseEntity.status(...)` | `ProblemDetail.status` |
| `ResponseEntity<ApiErrorResponse>` 반환 | `ResponseEntity.status(...)` | 커스텀 DTO 설계에 따라 다름 |
| `ErrorResponse` 또는 `ErrorResponseException` 사용 | `ErrorResponse.getStatusCode()` | `ErrorResponse.getBody().getStatus()`와 맞아야 함 |

가장 중요한 점은 **실제 HTTP 상태 코드와 body에 표현된 상태 코드가 서로 다르지 않게 유지하는 것**이다.

---

## 이번 PR에서 적용할 수 있는 개선 방향

현재 PR에서는 두 가지 방향 중 하나를 선택할 수 있다.

### 1. 최소 수정: 현재 구조 유지

현재 구조를 유지한다면 직접 만든 `ErrorResponse` 이름을 변경한다.

```text
ErrorResponse -> ApiErrorResponse
```

이렇게 하면 Spring의 `org.springframework.web.ErrorResponse`와 이름이 겹치지 않아 의도가 더 명확해진다.

```java
public record ApiErrorResponse(
        String message,
        Map<String, String> fields
) {
    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse(message, null);
    }

    public static ApiErrorResponse of(String message, Map<String, String> fields) {
        return new ApiErrorResponse(message, fields);
    }
}
```

### 2. 학습 반영: ProblemDetail로 전환

리뷰 내용을 학습하고 반영하는 목적이라면 커스텀 응답 객체 대신 `ProblemDetail`을 사용할 수 있다.

```java
@ExceptionHandler(CustomBadRequestException.class)
public ProblemDetail badRequest(CustomBadRequestException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            e.getMessage()
    );

    problemDetail.setTitle("Bad Request");

    return problemDetail;
}
```

이 방식은 Spring에서 제공하는 표준 에러 응답 구조를 사용할 수 있다는 장점이 있다.

### 3. 헤더 제어 필요 시: ResponseEntity와 ProblemDetail 사용

에러 응답에 별도 헤더를 추가해야 하거나 HTTP 응답 전체를 명시적으로 제어하고 싶다면 `ResponseEntity<ProblemDetail>`을 사용할 수 있다.

```java
@ExceptionHandler(CustomBadRequestException.class)
public ResponseEntity<ProblemDetail> badRequest(CustomBadRequestException e) {
    HttpStatus status = HttpStatus.BAD_REQUEST;

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            status,
            e.getMessage()
    );
    problemDetail.setTitle("Bad Request");

    return ResponseEntity
            .status(status)
            .header("X-Error-Code", "BAD_REQUEST")
            .body(problemDetail);
}
```

이 경우 `ProblemDetail`의 `status`와 `ResponseEntity`의 `status`를 반드시 일치시키는 것이 좋다.

---

## 결론

현재 구현은 `ResponseEntity`와 직접 만든 커스텀 에러 DTO를 사용하는 방식이다. 이 방식도 잘못된 것은 아니지만, Spring 6 이후에는 `ProblemDetail`과 `org.springframework.web.ErrorResponse`를 활용한 표준화된 에러 응답 방식도 제공된다.

따라서 이번 리뷰를 통해 다음 내용을 확인했다.

- `ProblemDetail`은 Spring에서 제공하는 표준 에러 응답 객체이다.
- `ProblemDetail`만 반환하면 `ProblemDetail.status`가 실제 HTTP 상태 코드 기준이 된다.
- `ResponseEntity<ProblemDetail>`을 사용하면 실제 HTTP 상태 코드는 `ResponseEntity.status(...)`가 기준이 된다.
- 따라서 `ResponseEntity.status(...)`와 `ProblemDetail.status`가 불일치하지 않도록 주의해야 한다.
- `org.springframework.web.ErrorResponse`는 Spring에서 제공하는 에러 응답 인터페이스이다.
- 직접 만든 `ErrorResponse`는 Spring의 `ErrorResponse`와 이름이 겹치므로 `ApiErrorResponse` 같은 이름으로 변경하는 것이 더 명확하다.
- `500 Internal Server Error` 같은 알 수 없는 예외에서는 내부 예외 메시지를 그대로 응답하지 않고 일반화된 메시지를 내려주는 것이 안전하다.

이번 PR에서는 최소 수정으로 간다면 `ErrorResponse`를 `ApiErrorResponse`로 변경하고, 추가 학습 또는 구조 개선 목적이라면 `ProblemDetail` 방식으로 전환하는 것이 적절하다고 판단했다.
