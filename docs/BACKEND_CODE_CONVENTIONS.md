# 📐 따숨 백엔드 코드 컨벤션

> 이 문서는 **"규칙"** 문서입니다. 규칙을 바꾸려면 팀 합의 후 이 문서를 수정하는 PR을 올려주세요.
> 기준 코드: `auth`, `member`, `common`, `dashboard`, `statistics` 패키지 (이미 구현된 코드가 컨벤션의 살아있는 예시입니다)

---

## 1. 패키지 구조

**도메인형 구조**를 사용합니다. 계층형(controller/service/repository 최상위) 구조는 사용하지 않습니다.

```
com.paw.ddasoom
├── common                  # 도메인에 속하지 않는 공통 모듈
│   ├── config              # @Configuration 클래스 (RedisConfig, MinioConfig, SwaggerConfig, QueryDslConfig 등)
│   ├── dto                 # ApiResponse, PageResponse 등 공통 DTO
│   ├── exception           # BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── security            # SecurityConfig, SecurityConstants, AuthJwtTokenFilter 등 시큐리티 인프라
│   └── util                # BaseTimeEntity, SecurityUtil 등
└── {domain}                # auth, member, dashboard, statistics, animal, foster, board, report ...
    ├── controller
    ├── service
    ├── repository
    ├── domain              # JPA 엔티티, Enum
    ├── dto
    │   ├── request         # XxxRequest
    │   └── response        # XxxResponse
    ├── exception           # {Domain}Exception, {Domain}ErrorCode
    └── util                # 해당 도메인 전용 유틸 (예: auth/util/MailUtil)
```

### 도메인 경계 기준

> **"어느 서비스에서 예외를 throw 하는가"** 가 그 코드의 소속 도메인입니다.

- 예: 회원가입/이메일 인증/로그인은 **auth** 소속 (AuthException 발생 지점)
- 예: 회원 정보 수정/탈퇴는 **member** 소속 (MemberException 발생 지점)
- 엔티티는 리소스를 소유한 도메인에 둡니다. `Member` 엔티티는 member 소속이며,
  auth가 이를 import하여 사용하는 **단방향 의존(auth → member)** 은 허용합니다. 역방향은 금지.

### 집계/통계 도메인 규칙 (dashboard · statistics)

- **여러 도메인에 걸친 읽기 전용 집계는 `statistics/repository/StatisticsQueryRepository`(QueryDSL) 한 곳에 모읍니다.**
  타 도메인 Repository에 집계 메서드를 흩뿌리지 않습니다.
- 집계 저장소는 **타 도메인 엔티티를 읽기만** 합니다(단방향 읽기 의존). 쓰기는 절대 하지 않습니다.
- dashboard(지금 처리할 일 — 액션 지표)와 statistics(기간 추세 분석)는 목적이 다르지만
  같은 집계 저장소를 공유합니다.

### 유틸 위치 기준

- 사용처가 **한 도메인뿐**이면 해당 도메인 하위에 둡니다. (예: `auth/util/MailUtil`)
- **두 도메인 이상**에서 쓰이는 시점에 `common/util`로 승격합니다. 미리 옮기지 않습니다.

---

## 2. 네이밍

### DTO

| 종류 | 규칙 | 예시 |
|------|------|------|
| 요청 DTO | `{행위/대상}Request` | `SignupRequest`, `AuthCodeSendRequest`, `SocialExtraInfoRequest` |
| 응답 DTO | `{행위/대상}Response` | `SignupResponse`, `LoginResponse` |
| Entity → DTO 변환 | 정적 팩토리 메서드 `from(Entity)` | `SignupResponse.from(member)` |
| DTO → Entity 변환 | 인스턴스 메서드 `toEntity(...)` | `signupRequest.toEntity(passwordEncoder)` |

- 요청 DTO는 `@Getter` + `@NoArgsConstructor` 만 사용합니다. (`@Setter` 금지 — 역직렬화는 필드 리플렉션으로 동작)
- 응답 DTO는 `@Getter` + `@Builder` + 정적 팩토리 `from()` 조합을 사용합니다. 생성자 직접 호출 대신 `from()`을 사용합니다.
- 여러 필드를 담는 응답에 `record`를 써도 됩니다(불변·간결). 정적 팩토리(`from`/`of`)와 병행 가능.

### 서비스 메서드

| 동사 | 의미 | 예시 |
|------|------|------|
| `get{X}` | 조회 — **없으면 예외 throw** | `getMember(id)` → 없으면 `MEMBER_NOT_FOUND` |
| `find{X}` | 조회 — 없을 수 있음, `Optional` 반환 | `findMemberByEmail(email)` |
| `create` / `update` / `delete` | 생성 / 수정 / 삭제 | `updateExtraInfo(...)` |
| 도메인 동사 | 비즈니스 의미가 명확한 동사 우선 | `signup`, `sendAuthCode`, `verifyAuthCode`, `softDelete` |

- CRUD 동사보다 **비즈니스 언어**가 우선입니다. `createMember`보다 `signup`이 낫습니다.
- boolean 반환 메서드는 `is`/`exists`/`has` 접두사를 사용합니다. (예: `existsByEmail`)
- **활성/전체 조회를 구분**하는 경우 이름으로 드러냅니다.
  (예: `getMember`(탈퇴면 예외) vs `getMemberIncludingDeleted`(관리자 전용, 탈퇴 포함))

### 엔티티

- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` + `@Builder` 생성자 조합. `@Setter` **전면 금지**.
- 상태 변경은 의미 있는 이름의 **리치 도메인 메서드**로만 수행합니다.
  (예: `member.softDelete()`, `member.changeStatus(HIDDEN)`, `member.updateExtraInfo(...)` — `setDeleted(true)` 같은 setter 금지)
- 생성/수정 시각이 필요한 엔티티는 `BaseTimeEntity`를 상속합니다. 시각 값은 DB가 생성하며(DEFAULT/ON UPDATE) 앱에서 세팅하지 않습니다 — `@CreatedDate` 등 JPA Auditing 어노테이션은 사용 금지(리스너 미등록으로 동작하지 않음).

---

## 3. 문법 스타일

### 삼항 연산자

**한 줄로 읽히는 기본값 대입/단순 분기에만 허용, 중첩 금지.**

```java
// ✅ 허용 — 기본값 대입
this.role = role != null ? role : Role.GUEST;
String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "입력값이 올바르지 않습니다.";

// ❌ 금지 — 중첩 삼항
String grade = score > 90 ? "A" : score > 80 ? "B" : "C";   // if-else 또는 switch로 작성

// ❌ 금지 — 삼항 안에서 메서드 호출 체인 등 복잡한 로직
return isValid ? repository.save(request.toEntity(encoder)) : handleInvalid(request);
```

### 부정 연산 (`!` vs `== false` / `!=`)

- **null 비교는 항상 `== null` / `!= null`** 로 명시합니다. `Objects.isNull()`은 사용하지 않습니다.
- **boolean 메서드/변수 하나에 대한 부정은 `!`** 를 사용합니다. 단, `!`가 묻히지 않도록 피연산자와 붙여 씁니다.

```java
// ✅ 허용
if (storedCode == null || !storedCode.equals(code)) { ... }
if (!memberRepository.existsByEmail(email)) { ... }

// ❌ 지양 — 이중 부정, 읽기 어려운 부정
if (!(a != null && !b.isEmpty())) { ... }   // 조건을 뒤집거나 변수로 추출할 것
```

- 복잡한 조건은 **의미 있는 이름의 지역 변수나 private 메서드로 추출**합니다.

```java
// ✅ 조건 추출 예시
boolean isCodeMismatch = storedCode == null || !storedCode.equals(code);
if (isCodeMismatch) {
    throw new AuthException(AuthErrorCode.INVALID_AUTH_CODE);
}
```

### 기타

- 의존성 주입은 **생성자 주입 + `@RequiredArgsConstructor`** 만 사용합니다. `@Autowired` 필드 주입 금지.
- 긴 문자열(HTML 템플릿 등)은 **텍스트 블록(`"""`)** + `formatted()`를 사용합니다. StringBuilder 연쇄 지양. (프로젝트 언어 레벨: **Java 21**)
- 매직 넘버/문자열은 `private static final` 상수로 선언하고 이유를 주석으로 남깁니다.
  (예: `AUTH_CODE_TTL = Duration.ofMinutes(3); // 메일 문구 "3분"과 일치`)
- Redis 키는 `{용도}:{식별자}` 형식이며, 키 생성은 private 메서드로 모아둡니다.
  (예: `authCode:{email}`, `verified:{email}`, `blacklist:{jti}`)

### 주석 (신규)

- 주석은 **"무엇을"이 아니라 "왜"** 를 설명합니다. 코드가 이미 말하는 동작은 반복하지 않습니다.
  (예: `// 탈퇴 회원도 새 AT를 못 받도록 명시적으로 막는다` ○ / `// member가 삭제됐는지 확인` ✗)
- 자명한 getter·단순 위임 메서드에는 개별 주석을 달지 않습니다(주석 과잉 지양). 필요하면 블록 단위로 한 번만.
- 보안/설계상 **트레이드오프**(fail-close, 응답 단일화, 캐시 TTL 근거 등)는 주석으로 남깁니다 — 나중에 "왜 이렇게 했지"를 막기 위함.

---

## 4. API 공통 규격

### URL 규칙

- prefix: `/api/{도메인}` (예: `/api/auth`, `/api/members`, `/api/admin/statistics`)
- 리소스는 **복수형 명사**, 행위는 HTTP Method로 표현합니다. 단, 인증처럼 행위 중심 API는 동사 경로를 허용합니다.
  (예: `/api/auth/signup`, `/api/auth/email/send`)
- 본인 리소스는 `/me`를 사용합니다. (예: `GET /api/members/me`)
- 관리자 API는 `/api/admin/{도메인}` 하위에 둡니다 — SecurityConfig가 이 경로를 ADMIN 전용으로 자동 잠급니다.

### 응답 규격 — 모든 응답은 `ApiResponse<T>` 봉투 하나

```json
// 성공
{ "code": "SUCCESS", "message": "회원가입이 완료되었습니다.", "data": { ... } }

// 실패 (비즈니스 예외)
{ "code": "AUTH_004", "message": "인증 번호가 일치하지 않거나 만료되었습니다.", "data": null }

// 실패 (@Valid 검증)
{ "code": "INVALID_INPUT", "message": "비밀번호는 대소문자, 숫자, 특수문자 포함 8자 이상이어야 합니다.", "data": null }
```

- 성공 판별 기준은 `code === "SUCCESS"` **하나**입니다.
- HTTP 상태 코드는 전송 레벨(200/201/400/404/409/500), `code`는 비즈니스 레벨로 역할을 나눕니다. 둘 다 정확히 내려줍니다.
- 컨트롤러 반환 타입은 `ResponseEntity<ApiResponse<T>>`로 통일합니다.
- 페이징 응답은 `PageResponse<T>`(공통 DTO)로 감쌉니다.

```java
// ✅ 성공 응답 작성법
return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다."));
return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("회원가입이 완료되었습니다.", response));

// ❌ 금지 — 컨트롤러에서 error() 직접 호출
return ResponseEntity.badRequest().body(ApiResponse.error(...));   // 예외를 throw 하세요
```

### 컨트롤러의 책임 경계 (신규)

- 컨트롤러는 **얇게** 유지합니다 — 요청 파싱 / 서비스 호출 / 응답 조립만.
- 로그인 회원 식별은 컨트롤러가 토큰에서 꺼내 **memberId를 서비스에 파라미터로 전달**합니다.
  서비스가 `SecurityContextHolder`를 직접 참조하지 않습니다. (6번 참고)
- `HttpServletRequest`에서만 얻을 수 있는 값(clientIp 등)은 **컨트롤러가 추출해 서비스에 넘깁니다.**
  서비스가 `HttpServletRequest`를 의존하지 않게 합니다. (예: `resolveClientIp(request)` → `authService.sendAuthCode(email, clientIp)`)

### HTTP 상태 코드 기준

| 상태 | 사용 시점 |
|------|-----------|
| 200 | 조회, 수정, 처리 성공 |
| 201 | 리소스 생성 (회원가입, 게시글 작성 등) |
| 400 | 유효성 실패, 잘못된 요청 (INVALID_INPUT, AUTH_003, AUTH_004 등) |
| 401 | 미인증 (토큰 없음/만료) |
| 403 | 권한 부족 (GUEST가 USER 전용 API 접근 등) |
| 404 | 리소스 없음 (MEMBER_NOT_FOUND 등) |
| 409 | 중복 충돌 (EMAIL_ALREADY_EXISTS 등) |
| 429 | 요청 빈도 제한 (AUTH_006 재발송 쿨다운 / AUTH_007 IP 발송 제한 등) |
| 500 | 서버 오류 (MAIL_SEND_FAILED, GLOBAL_ERROR) |

---

## 5. 예외 처리 공통 규격

### 구조

```
RuntimeException
└── BusinessException (common.exception)          ← 모든 비즈니스 예외의 부모
    ├── AuthException   + AuthErrorCode (enum)
    ├── MemberException + MemberErrorCode (enum)
    └── {Domain}Exception + {Domain}ErrorCode      ← 새 도메인은 이 패턴으로 추가
```

- `ErrorCode` 인터페이스(`getStatus()`, `getCode()`, `getMessage()`)를 구현한 **enum**으로 에러를 정의합니다.
- `GlobalExceptionHandler`가 `BusinessException`을 잡아 `ApiResponse.error()`로 자동 변환합니다.
  → **서비스는 throw만 하면 되고, 컨트롤러는 try-catch를 쓰지 않습니다.**
- Spring 표준 예외(`MethodArgumentNotValid`, `MethodArgumentTypeMismatch`, `MissingServletRequestParameter`,
  `MaxUploadSizeExceeded` 등)도 `GlobalExceptionHandler`에서 400 `INVALID_INPUT` 규격으로 변환합니다 — 개별 컨트롤러에서 처리하지 않습니다.

### 에러 코드 네이밍

- 코드 문자열: `{도메인 대문자}_{3자리 번호}` (예: `AUTH_001`, `MEMBER_001`)
- enum 상수명: 상황을 설명하는 대문자 스네이크 (예: `EMAIL_ALREADY_EXISTS`, `INVALID_AUTH_CODE`)
- 번호는 도메인별로 001부터 순차 부여. **프론트와 협의된 코드는 번호를 바꾸지 않습니다.**
  (새 상황이 생기면 기존 코드를 재활용하기보다 새 번호를 부여해 프론트 문구 매핑 충돌을 피합니다.)

### 새 도메인 예외 추가 방법 (3단계)

```java
// 1. ErrorCode enum 생성 — {domain}/exception/FosterErrorCode.java
@Getter
@RequiredArgsConstructor
public enum FosterErrorCode implements ErrorCode {
    FOSTER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOSTER_001", "해당 임시보호 신청을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

// 2. Exception 클래스 생성 — {domain}/exception/FosterException.java
public class FosterException extends BusinessException {
    public FosterException(FosterErrorCode errorCode) {
        super(errorCode);
    }
}

// 3. 서비스에서 throw — 핸들러가 알아서 응답으로 변환
throw new FosterException(FosterErrorCode.FOSTER_NOT_FOUND);
```

### 예외 사용 규칙

- **`RuntimeException`을 직접 throw 하지 않습니다.** 반드시 도메인 Exception + ErrorCode 조합을 사용합니다.
- 다른 도메인의 예외를 던지는 것은 그 도메인의 사실을 표현할 때 허용합니다.
  (예: AuthService가 "회원 없음"을 표현할 때 `MemberException(MEMBER_NOT_FOUND)` 사용 가능)
- 만료와 불일치처럼 **보안상 구분을 노출하면 안 되는 경우 하나의 에러코드로 합칩니다.**
  (예: `INVALID_AUTH_CODE` — "일치하지 않거나 만료되었습니다", `INVALID_RESET_TOKEN` — 토큰 무효/만료/탈퇴 회원 통합)
- **Security 필터 체인 내부(OAuth2 등)에서는 BusinessException을 던지지 않습니다.**
  GlobalExceptionHandler에 닿지 못해 500이 됩니다 — `OAuth2AuthenticationException`으로 감싸 FailureHandler가 처리하게 합니다.

---

## 6. 트랜잭션 & 데이터 접근

- `@Transactional`은 **서비스 계층에만** 붙입니다. 컨트롤러/레포지토리에는 금지.
- DB 쓰기가 없는 메서드에는 붙이지 않습니다. (Redis 조작은 JPA 트랜잭션 대상이 아님)
- 조회 전용 메서드는 `@Transactional(readOnly = true)`를 사용합니다.
- 외부 I/O(메일 발송 등)가 트랜잭션 실패로 이어지면 안 되는 경우, try-catch로 격리하고 로그만 남깁니다.
  (예: 회원가입 성공 후 환영 메일 실패 → 가입은 성공 처리)
- 반대로 **원자성이 필요한 경우**(탈퇴 = soft delete + 세션 정리)는 한 트랜잭션에 묶어, 일부 실패 시 전체 롤백되게 합니다.
- Repository 메서드는 Spring Data JPA 쿼리 메서드 네이밍을 따릅니다. (`findByEmail`, `existsByNickname`)
  동적·복합 조건은 QueryDSL을 사용합니다.
- **`@Async` 등 별도 스레드에서 `SecurityUtil.getMemberId()`(또는 `SecurityContextHolder` 직접 접근)를 호출하지 않습니다.**
  `SecurityContextHolder`는 ThreadLocal 기반이라 새로 생성된 스레드에는 인증 컨텍스트가 없어 즉시 예외가 발생합니다.
  비동기 로직이 필요하면 부모 스레드에서 memberId 등 필요한 값을 추출해 **파라미터로 전달**하세요.
- **서비스 메서드는 memberId(및 필요한 원시값)를 파라미터로 받습니다.**
  SecurityContext 직접 의존을 피해 테스트 가능성·스레드 안전성을 확보하기 위한 프로젝트 표준입니다.

---

## 7. 로깅

- `@Slf4j` 를 사용합니다. `System.out.println`, `e.printStackTrace()` **금지**.
- 레벨 기준:
  - `error` — 예외 상황, 스택트레이스 포함 (`log.error("메일 전송 실패 - 수신자: {}", to, e)`)
  - `warn` — 처리는 됐지만 주의가 필요한 상황 (환영 메일 실패 등)
  - `debug` — 개발 확인용 (운영에서는 출력 안 됨). 인증 실패 사유 등은 debug로.
- **비밀번호, 인증 코드 원문, 토큰은 info 이상 레벨에 남기지 않습니다.**

---

## 8. API 문서 (Swagger / OpenAPI)

- 컨트롤러에 springdoc 애노테이션으로 문서를 남깁니다: 클래스 `@Tag`, 엔드포인트 `@Operation` + `@ApiResponses` + `@Parameter`.
- description에 **인가 요건**(공개 / USER / ADMIN)과 **비즈니스 에러코드**를 함께 적어 명세만으로 동작을 파악하게 합니다.
- ⚠️ **이름 충돌 주의**: 프로젝트 공통 `common.dto.ApiResponse`와 Swagger의 `ApiResponse`가 동명입니다.
  Swagger 쪽은 import하지 말고 **FQN**(`@io.swagger.v3.oas.annotations.responses.ApiResponse`)으로 씁니다.
- 인증이 필요한 컨트롤러는 클래스에 `@SecurityRequirement(name = "bearerAuth")`를 붙이고,
  그 안에서 예외적으로 인증이 필요 없는 API만 `@Operation(security = {})`로 해제합니다.
- 접속: `http://localhost:8080/swagger-ui.html` (백엔드 포트로 직접).
- 레퍼런스: `auth/controller/AuthController.java`.

---

## 9. Git & PR

- 브랜치: `feat/{도메인}-{작업}` (예: `feat/auth-signup`). 문서/리팩토링은 `docs/`, `refactor/` 접두사.
- PR 템플릿: 🚀 개요 / 🛠️ 작업 내용 / 📋 팀원 가이드(필수 확인 사항) 구조를 유지합니다.
- 응답 포맷·에러 코드 등 **프론트에 영향 있는 변경은 PR에 ⚠️ 표시**하고 프론트 담당자 확인을 받습니다.
- **타 도메인(남의 `features`/패키지) 코드는 PR 없이 수정하지 않습니다.** 연동이 필요하면 협업 요청 문서로 전달합니다.
- `application.yml` 등 시크릿 파일은 커밋 금지. 시크릿은 환경변수(`DDASOOM_*`)로 주입하며, yml에는 `${...}` 플레이스홀더만 둡니다.
- DB 변경은 `db/migration`에 **새 버전(Vn)** 으로 추가합니다. 이미 머지된 마이그레이션은 수정하지 않습니다.
  로컬 더미는 `db/seed`(V100~)에서 관리하며 자유롭게 재작성합니다.
