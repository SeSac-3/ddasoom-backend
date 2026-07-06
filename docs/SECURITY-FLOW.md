# 🔐 따숨 Security Flow

> 인증/인가가 어떻게 동작하는지, 그리고 **각 도메인 개발자가 컨트롤러에서 로그인한 회원 정보를 어떻게 꺼내 쓰는지** 안내하는 문서입니다.
>
> ⚠️ **현재 구현 상태**: 이메일 인증 회원가입까지 구현 완료. JWT 로그인/로그아웃, SNS 로그인은 **설계 확정 후 구현 예정**이며, 아래 [예정] 표시된 부분은 구현 시점에 갱신됩니다.
>
> 🔑 **토큰 정책 최종 결정 (2026-07)**:
> - **Access Token** → 서버에 저장하지 않으며(로그아웃 블랙리스트만 예외), 클라이언트는 **전역 상태 변수**(zustand/recoil 등 상태관리 저장소)로 보관. 매 요청 `Authorization: Bearer` 헤더로 전송. **localStorage / sessionStorage / 쿠키 저장 금지.**
> - **Refresh Token** → **HttpOnly 쿠키** + **Redis**(`refresh:{memberId}`) 이중 관리. 프론트 JS에서는 접근 불가.
> - **RT 로테이션 적용** → reissue 시 RT도 새로 발급하며, 멀티탭 동시 재발급 경합은 **서버 측 grace period(30초)** 로 흡수합니다.
>
> (결정 배경과 비교 검토안은 하단 부록 참고)

---

## 1. 권한(Role) 체계

| Role | 대상 | 접근 범위 |
|------|------|-----------|
| `GUEST` | SNS 가입 후 **추가정보 미입력** 회원 | 추가정보 입력 API만 접근 가능 |
| `USER` | 일반 가입 완료 회원, SNS 가입 + 추가정보 입력 완료 회원 | 일반 서비스 전체 |
| `ADMIN` | 초기 데이터로 생성되는 관리자 | `/api/admin/**` 포함 전체 |

- 일반 회원가입은 즉시 `USER`로 생성됩니다.
- SNS 가입자는 `GUEST`로 생성 → 추가정보(닉네임/실명/전화번호) 입력 완료 시 `USER`로 승급됩니다. (`member.updateExtraInfo()`)
- **ADMIN은 회원가입 API로 만들 수 없습니다.** 초기 시드 데이터(SQL)로만 생성합니다.

---

## 2. 전체 인증 흐름 요약

```
[일반 회원가입 - 구현 완료]
이메일 입력 → POST /api/auth/email/send   (인증코드 메일 발송, Redis authCode:{email} TTL 3분)
코드 입력   → POST /api/auth/email/verify  (검증 성공 시 verified:{email} TTL 30분)
정보 입력   → POST /api/auth/signup        (verified 확인 → BCrypt 인코딩 → DB 저장 → 201)

[로그인 - 예정]
POST /api/auth/login
  → 이메일/비밀번호 검증 (AuthenticationManager + BCrypt)
  → 응답 body: AT + 만료시간(초) + 회원 요약 정보   ★ 비밀번호 해시 등 민감정보 절대 미포함
  → Set-Cookie: RT (HttpOnly)
  → Redis: refresh:{memberId} 에 RT 저장 (TTL 14일)
  → 로그인 로그 기록 (LoginLog — 실패해도 로그인은 성공 처리)

[인증이 필요한 API 호출 - 예정]
프론트가 상태 변수의 AT를 Authorization: Bearer {AT} 헤더에 실어 전송
  → JwtAuthenticationFilter가 헤더에서 AT 추출·검증
  → 블랙리스트(blacklist:{jti}) 확인
  → claims만으로 인증 객체 구성 (매 요청 DB 조회 없음)
  → SecurityContext 등록 → 컨트롤러에서 회원 정보 추출 가능 (아래 4번)

[토큰 재발급 - 예정]  ★ RT 로테이션
POST /api/auth/reissue   (AT 만료 시 / 앱 부팅 시)
  → 쿠키의 RT 파싱·검증
  → Redis(refresh:{memberId}) 와 대조
      ├─ 일치 → 통과
      ├─ 불일치 → graceRefresh:{memberId} 와 한 번 더 대조 (30초 유예 창)
      │     ├─ 일치 → 통과 (직전에 다른 탭이 회전시킨 구 RT — 정상 사용자)
      │     └─ 불일치 → 401 (재로그인)
      └─ 키 없음 → 401 (재로그인)
  → 통과 시:
      새 AT (응답 body) + 새 RT (Set-Cookie) 발급
      refresh:{memberId} ← 새 RT 로 교체 (TTL 14일)
      graceRefresh:{memberId} ← 구 RT 저장 (TTL 30초)

[로그아웃 - 예정]
POST /api/auth/logout (인증 필요 — Authorization 헤더 필수)
  → Redis의 refresh:{memberId} 및 graceRefresh:{memberId} 삭제
  → AT의 jti를 남은 유효시간만큼 블랙리스트(blacklist:{jti}) 등록
  → RT 쿠키 삭제 (Max-Age=0)
  → 프론트는 상태 변수의 AT 폐기
```

---

## 3. 엔드포인트 접근 정책

`SecurityConfig` + `SecurityConstants.PUBLIC_URIS`에서 관리합니다.

| 경로 | 접근 정책 | 상태 |
|------|-----------|------|
| `/api/auth/email/send`, `/email/verify`, `/signup` | permitAll | 구현 완료 |
| `/api/auth/login`, `/api/auth/reissue` | permitAll | 예정 |
| `/api/auth/logout` | **authenticated** (⚠️ auth 하위지만 토큰 필요) | 예정 |
| `/api/members/me/signup-complete` (SNS 추가정보) | hasRole("GUEST") | 예정 |
| `/api/members/**` | hasAnyRole("USER", "ADMIN") | 예정 |
| `/api/admin/**` | hasRole("ADMIN") | 구성됨 |
| 그 외 | authenticated | 구성됨 |

> ⚠️ **현재는 개발 초기라 `PUBLIC_URIS = "/api/**"` 로 전부 열려 있습니다.**
> 기능 구현이 진행되면 위 표대로 좁혀갑니다. 즉 **지금 인증 없이 되는 API도 나중에 토큰이 필요해질 수 있으니**, 프론트 연동 시 이 표를 기준으로 개발해 주세요.
>
> ⚠️ `requestMatchers`는 **선언 순서대로** 매칭됩니다. 구체적 경로(logout, GUEST 규칙)를 넓은 경로보다 먼저 선언해야 합니다. SecurityConfig 주석의 번호 순서를 지켜주세요.

---

## 4. [팀원 필독] 컨트롤러에서 로그인 회원 정보 꺼내기 [예정 — 시그니처 확정 후 갱신]

로그인 구현 완료 후, 인증이 필요한 API에서는 아래 방법으로 "지금 요청한 회원"을 식별합니다.

### 설계 방침

- JWT의 `subject`에는 **회원 PK(memberId)** 를 담습니다. (이메일이 아닌 PK — 이메일 변경 기능이 생겨도 토큰이 무효화되지 않도록)
- 필터는 **토큰 claims만으로** `CustomUserDetails`(memberId, email, role)를 구성해 SecurityContext에 등록합니다. **매 요청 DB 조회는 하지 않습니다.** (권한 변경의 즉시 반영은 AT 30분 만료로 수용)

### 방법 A — `@AuthenticationPrincipal` (권장)

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
        @AuthenticationPrincipal CustomUserDetails userDetails) {

    Long memberId = userDetails.getMemberId();   // ← 회원 PK
    MemberResponse response = memberService.getMyInfo(memberId);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

### 방법 B — `SecurityUtil` (서비스 계층 등 파라미터 전달이 번거로운 곳)

```java
Long memberId = SecurityUtil.getMemberId();   // SecurityContext에서 직접 추출
```

### 사용 규칙

1. **컨트롤러에서는 방법 A를 기본**으로 사용합니다. 파라미터로 명시되어 테스트와 가독성에 유리합니다.
2. 서비스 메서드는 `memberId`를 **파라미터로 받도록** 작성합니다. 서비스가 SecurityContext에 직접 의존하면 테스트가 어려워집니다.
3. 회원 엔티티가 필요하면 서비스에서 조회합니다.
   ```java
   Member member = memberRepository.findById(memberId)
           .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
   ```
4. **요청 body나 쿼리 파라미터로 memberId를 받지 마세요.** 항상 토큰에서 추출한 memberId를 사용합니다. (body의 memberId를 믿으면 타인 정보 조작이 가능해집니다)

---

## 5. 토큰 정책 [예정 — 구현 시 확정]

| 항목 | Access Token | Refresh Token |
|------|--------------|---------------|
| 클라이언트 저장 | **전역 상태 변수** (상태관리 저장소) — localStorage/쿠키 금지 | **HttpOnly 쿠키** |
| 서버 저장 | 없음 (로그아웃 시 jti 블랙리스트만 예외) | **Redis** (`refresh:{memberId}`) — reissue 시 대조 |
| 전송 방식 | 매 요청 `Authorization: Bearer {AT}` 헤더 | 브라우저 자동 전송 (쿠키) |
| 쿠키 Path | 해당 없음 | `/api/auth` (reissue/logout에만 전송) |
| 유효 기간 | 30분 (예정) | 14일 (예정) |
| 재발급 시 | 새로 발급 | **새로 발급 (로테이션)** — 구 RT는 30초 grace 후 완전 폐기 |
| 담는 정보 | memberId(sub), role, category("access"), jti | memberId(sub), category("refresh"), jti |
| 만료 시 | `POST /api/auth/reissue`로 재발급 | 재로그인 |

### RT 쿠키 속성

| 속성 | 값 | 이유 |
|------|-----|------|
| `HttpOnly` | true | JS 접근 불가 → XSS로 RT 반출 차단 |
| `SameSite` | `Lax` | 타 사이트발 요청에 쿠키 미전송 → CSRF 방어 |
| `Secure` | 운영 true / 로컬 false | HTTPS 전용 (프로파일로 분기) |
| `Path` | `/api/auth` | RT가 필요한 곳(reissue/logout) 외 전송 차단 |

- AT는 헤더 방식이므로 **CSRF에 해당 없음** (공격자가 타 사이트에서 헤더를 심을 수 없음). 쿠키 방식 대비 배포 도메인 제약(same-site)에서 자유롭습니다.
- `category` claim으로 AT/RT를 구분합니다. RT를 AT 자리에 꽂는 오용을 필터에서 차단합니다.

### RT 로테이션 + grace period (30초)

- **로테이션 적용**: reissue 성공 시마다 RT를 새로 발급합니다. 탈취된 RT는 정상 사용자의 다음 reissue 시점에 무효화되어 수명이 짧아집니다.
- **grace period가 필요한 이유**: RT 쿠키는 탭 간 공유되므로, 브라우저 재시작으로 여러 탭이 동시에 마운트되면 **같은 구 RT를 든 reissue 요청이 동시에** 들어옵니다. 첫 요청이 RT를 회전시키면 나머지 요청의 구 RT는 대조 실패 → 멀쩡한 탭이 전부 로그아웃되는 경합이 발생합니다.
- **해결**: 회전 직후 구 RT를 `graceRefresh:{memberId}` (TTL 30초)에 보관하고, 주 키 대조 실패 시 grace 키와 한 번 더 대조합니다. 30초 창 안의 동시 요청은 전부 정상 처리되며, 창이 닫히면 구 RT는 완전 폐기됩니다.
- **재사용 탐지(reuse detection)는 이번 범위에서 미적용**: grace 창과 개념이 상충하고(창 안의 구 RT는 정상 취급) 구현 복잡도가 올라, 보안 강화가 필요해지는 시점에 별도 작업으로 도입합니다.
- ⚠️ 구현 시 grace 처리에서 **구 RT로 통과한 요청은 RT를 다시 회전시키지 않습니다** (새 AT만 발급). 회전은 주 키 일치 시에만 수행해 grace 체인이 무한히 이어지는 것을 방지합니다.

### 팀 결정 대기 항목

| 항목 | 선택지 | 잠정 기본값 |
|------|--------|-------------|
| 다중 기기 로그인 | 단일 세션(`refresh:{memberId}`) vs 기기별(`refresh:{memberId}:{jti}`) | **단일 세션** — 새 로그인 시 기존 기기 세션 만료 |
| RT TTL 방식 | 고정 14일 vs 회전 시 갱신(슬라이딩) | **회전 시 갱신** — 로테이션 구조상 새 RT 발급 시 TTL 14일 재부여가 자연스러움. 14일간 미접속 시 재로그인 |

---

## 6. 프론트엔드 연동 가이드 [예정]

AT를 상태 변수로 관리하므로 **새 탭·새로고침 시 AT가 사라집니다.** 아래 ①②가 프론트 필수 구현입니다.

### 필수 설정

```js
// RT 쿠키가 reissue 요청에 실리도록 쿠키 전송 옵션 필수
axios.defaults.withCredentials = true;
```

### ① 부트스트랩 — 앱 마운트(새 탭/새로고침) 시 로그인 복원

```
앱 마운트 → POST /api/auth/reissue  (RT 쿠키 자동 전송)
  ├─ 200 → 응답의 새 AT를 상태 변수에 저장 → 로그인 상태로 렌더링
  └─ 401 → 비로그인 상태로 렌더링
```

- 부트스트랩이 끝나기 전에는 로그인 여부를 알 수 없으므로 **로딩 상태 처리**가 필요합니다. (판별 전 보호 라우트 진입 방지)
- **여러 탭이 동시에 마운트되어도(브라우저 재시작 등) 별도 처리가 필요 없습니다.** 동시 reissue 경합은 서버의 grace period(30초)가 흡수합니다. 프론트는 탭마다 독립적으로 부트스트랩하면 됩니다.

### ② 401 인터셉터 — AT 만료 처리

```
API 401 응답 → POST /api/auth/reissue
  ├─ 200 → 새 AT를 상태 변수에 갱신 → 원요청 1회 재시도
  └─ 401 (RT 만료/불일치) → 상태 초기화 후 로그인 페이지로
```

- reissue 실패 시 **무한 재시도 금지** (재시도 1회 제한 플래그 필수)
- reissue 자체의 401은 인터셉터가 다시 잡지 않도록 예외 처리해야 합니다.

### ③ 탭 간 동기화 (선택 — 프론트 담당 영역)

- 탭마다 상태 변수가 독립이므로, A탭의 로그인/로그아웃을 B탭 **화면에 즉시** 반영하려면 BroadcastChannel 등으로 프론트에서 동기화합니다.
- 동기화를 생략해도 서버 상태와의 최종 일치는 보장됩니다 — 로그아웃 후 다른 탭은 다음 reissue 시점에 401을 받아 자연 로그아웃됩니다. "즉시 반영"이 필요한 화면에서만 구현하면 됩니다.

### 기타 규칙

- **AT를 localStorage/sessionStorage에 백업하지 마세요.** 상태 변수 관리의 XSS 방어 이점이 사라집니다.
- 로그아웃: `POST /api/auth/logout` 호출(AT 헤더 포함) → 성공 후 상태 변수 초기화. RT 쿠키 삭제는 서버가 수행합니다.
- 백엔드 협의 사항: reissue 응답 형식/속도를 변경할 경우 부트스트랩·인터셉터에 직접 영향이 가므로 **반드시 사전 협의**합니다.

---

## 7. Redis 키 설계 (인증 관련 전체)

| 키 | 값 | TTL | 용도 | 상태 |
|----|----|-----|------|------|
| `authCode:{email}` | 6자리 코드 | 3분 | 이메일 인증 코드 | 구현 완료 |
| `verified:{email}` | "true" | 30분 | 인증 완료 ~ 가입 사이 유예 | 구현 완료 |
| `refresh:{memberId}` | 현재 유효한 RT | 14일 | reissue 시 쿠키 RT와 대조 (회전 시 교체) | 예정 |
| `graceRefresh:{memberId}` | 직전 회전된 구 RT | 30초 | 멀티탭 동시 reissue 경합 흡수 | 예정 |
| `blacklist:{jti}` | "logout" | AT 남은 시간 | 로그아웃된 AT 차단 (토큰 전문이 아닌 jti로 관리) | 예정 |

- TTL은 **RedisConfig가 아니라 서비스 계층에서** 지정합니다.
- 인증 코드 재발송 시 기존 `verified:{email}`이 삭제됩니다. (프론트의 "재인증 시 되돌리기"와 서버 상태 일치)
- 강제 로그아웃은 `refresh:{memberId}` + `graceRefresh:{memberId}` 삭제로 처리합니다. 삭제 즉시 해당 회원의 reissue가 차단됩니다. (이미 발급된 AT는 최대 30분간 유효 — 즉시 차단이 필요하면 블랙리스트 병행)

---

## 8. 인증 관련 에러 응답

모든 에러는 공통 규격 `{ code, message, data: null }`로 내려갑니다. ([컨벤션 문서](./CONVENTIONS.md) 참고)

| code | HTTP | 상황 |
|------|------|------|
| `AUTH_001` | 409 | 이미 가입된 이메일 (인증코드 발송/가입 시) |
| `AUTH_002` | 409 | 닉네임 중복 |
| `AUTH_003` | 400 | 이메일 인증 미완료 상태로 가입 시도 (30분 초과 포함) |
| `AUTH_004` | 400 | 인증 코드 불일치 또는 만료 |
| `AUTH_005` | 500 | 메일 전송 실패 |
| `MEMBER_001` | 404 | 회원 없음 |
| 로그인/토큰 에러 | 401 | 자격 증명 불일치, AT 만료/위조, RT 재발급 실패 [예정 — 구현 시 AUTH_1xx 대역으로 코드 확정] |
| 권한 부족 | 403 | GUEST의 USER API 접근 등 [예정] |

> ⚠️ [예정] 401/403은 Spring Security **필터 레벨**에서 발생하므로 `@RestControllerAdvice`(GlobalExceptionHandler)가 잡지 못합니다.
> `AuthenticationEntryPoint`(401) / `AccessDeniedHandler`(403)를 구현해 동일한 `ApiResponse` 포맷으로 내려줍니다. **필터에서 예외를 throw만 하고 EntryPoint를 안 만들면 500 HTML이 떨어지므로, 필터와 EntryPoint는 반드시 세트로 구현합니다.**

---

## 부록. 토큰 정책 결정 배경

### 저장 방식 (방식 A 채택)

| 방식 | 구성 | 채택 여부 / 사유 |
|------|------|------|
| **A. AT 상태 변수 + RT 쿠키/Redis** | **채택** | 글로벌 SPA 권장 표준 계열. AT가 헤더 방식이라 CSRF 무관, 배포 도메인 제약 없음. 프론트가 부트스트랩·인터셉터를 담당하는 것으로 팀 합의 완료 |
| B. AT/RT 모두 HttpOnly 쿠키 | 미채택 | 프론트 부담은 가장 적으나 프론트/백 same-site 배포 전제 필요. 팀 논의 결과 A로 결정 |
| C. AT 쿠키 + 세션만 Redis | 미채택 | 만료 AT를 갱신 자격증명으로 쓰는 커스텀 파싱 등 구현 특이점 존재 |

### RT 로테이션 (적용 + grace 30초)

- **적용 이유**: 탈취된 RT의 수명 단축. 이전 참고 프로젝트의 구조 계승.
- **grace period를 반드시 함께 넣는 이유**: 참고 프로젝트는 로테이션만 있고 grace가 없어, 멀티탭 동시 reissue 시 정상 사용자가 로그아웃되는 경합 버그를 안고 있었음. 서버 측 30초 유예 키 하나로 이 경합을 전부 흡수하며, 프론트는 추가 구현 없이 단순 부트스트랩만으로 안전함.
- **재사용 탐지 미적용**: grace 창과 상충 + 복잡도 대비 데모 범위에서 실익 낮음. 추후 별도 작업.
