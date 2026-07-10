# 🔐 따숨 Security Flow

> 인증/인가가 어떻게 동작하는지, 그리고 **각 도메인 개발자가 컨트롤러에서 로그인한 회원 정보를 어떻게 꺼내 쓰는지** 안내하는 문서입니다.
>
> ✅ **구현 상태**: 이메일 인증 회원가입, 일반 로그인/로그아웃, 토큰 재발급(RT 로테이션 + grace), SNS 로그인(카카오/네이버/구글), GUEST 추가정보 승급, 마이페이지(조회/수정/비밀번호 변경/탈퇴), 비밀번호 재설정 — **전부 구현 완료.** 남은 것은 관리자 회원 API(`/api/admin/members/**`)뿐입니다.
>
> 🔑 **토큰 정책 (확정)**:
> - **Access Token** → 서버 미저장(로그아웃 블랙리스트만 예외), 클라이언트는 **전역 상태 변수**(zustand) 보관, 매 요청 `Authorization: Bearer` 헤더. **localStorage / sessionStorage / 쿠키 저장 금지.**
> - **Refresh Token** → **HttpOnly 쿠키** + **Redis** 이중 관리. 프론트 JS 접근 불가.
> - **RT 로테이션 적용**, 멀티탭 동시 재발급 경합은 **서버 grace period(30초)** 가 흡수.

---

## 1. 권한(Role) 체계

| Role | 대상 | 접근 범위 |
|------|------|-----------|
| `GUEST` | SNS 가입 후 **추가정보 미입력** 회원 | 추가정보 입력 API만 |
| `USER` | 일반 가입 완료, SNS 가입 + 추가정보 완료 | 일반 서비스 전체 |
| `ADMIN` | 시드 데이터로만 생성 (`dummy/member-dummy.sql`, 비밀번호 `Ddasoom1!`) | `/api/admin/**` 포함 전체 |

- 일반 회원가입 → 즉시 `USER`. SNS 가입 → `GUEST`로 시작, 추가정보 입력 시 `USER` 승급.
- **ADMIN은 회원가입 API로 만들 수 없습니다.**

---

## 2. 전체 인증 흐름 (구현 완료 기준)

```
[일반 회원가입]
POST /api/auth/email/send    인증코드 발송 (재발송 겸용 — 기존 인증상태 무효화)
POST /api/auth/email/verify  코드 검증 → verified:{email} (30분)
POST /api/auth/signup        verified 확인 → BCrypt 저장(USER) → 201 + 환영 메일

[일반 로그인]
POST /api/auth/login
  → BCrypt 검증 (계정없음/비번틀림/탈퇴/소셜전용 전부 AUTH_101 하나 — 열거 공격 방지)
  → body: AT + expiresIn(초) + 회원요약  /  Set-Cookie: RT (HttpOnly)
  → Redis refresh:{memberId} 저장 + login_log 기록

[SNS 로그인 — 카카오/네이버/구글]
소셜 버튼 = <a href="/api/oauth2/authorization/{provider}">  ※ axios 금지, 페이지 이동
  → provider 동의 → 콜백(/api/login/oauth2/code/{provider})
  → provider+providerId 로 회원 판별 (신원은 이메일이 아님!)
      ├ 기존 연동 회원 → 로그인 (탈퇴 회원은 차단)
      ├ 신규 + 이메일 충돌 → AUTH_106 (수동 연동 정책 — 자동 연동 금지)
      └ 신규 → Member(GUEST, password=NULL) + MemberSocial 생성
  → 성공: RT 쿠키만 발급 후 {프론트}/oauth/callback 리다이렉트 (AT는 콜백의 reissue가 발급)
  → 실패: {프론트}/oauth/callback?error=AUTH_1xx

[인증 API 호출]
Authorization: Bearer {AT}
  → AuthJwtTokenFilter: 파싱 → category=access 확인 → 블랙리스트 확인
  → claims만으로 CustomUserDetails 구성 (매 요청 DB 조회 없음)
  ※ 권한 변경(GUEST→USER 등)은 reissue 후 새 AT부터 반영됨

[재발급 — RT 로테이션 + grace 30초]
POST /api/auth/reissue  (RT 쿠키 자동 전송)
  → Redis 주 키 대조 일치 → 새 AT + 새 RT(회전), 구 RT는 graceRefresh(30초)로
  → 주 키 불일치 → grace 키 대조 → 일치 시 새 AT만 (재회전 금지)
  → 둘 다 불일치 → 401 AUTH_104

[로그아웃]
POST /api/auth/logout  (authenticated — auth 하위의 유일한 예외)
  → refresh + grace 삭제, AT jti 블랙리스트, RT 쿠키 삭제

[GUEST 추가정보 → USER 승급]
PATCH /api/members/me/signup-complete  (hasRole GUEST)
  → 닉네임 중복 검사 → updateExtraInfo → USER 승급
  → ⚠️ 프론트: 성공 후 reissue 1회 필수 (AT의 role claim 갱신)

[비밀번호 재설정(찾기)]
POST /api/auth/password/reset-request  { email }
  → 이메일 존재 여부와 무관하게 항상 동일 성공 응답 (열거 공격 방지)
  → 대상일 때만 메일 발송: {프론트}/reset-password?token={uuid}  (Redis 30분)
POST /api/auth/password/reset  { token, newPassword }
  → 일회용 토큰 검증 → 변경 → 전 세션 무효화(재로그인 필요)
```

---

## 3. 인가 규칙 (확정 — 기본 잠금 + 등록제)

**원칙: 등록되지 않은 API는 토큰 필수가 기본값입니다.** 공개가 필요하면 담당자가 직접 `SecurityConstants.PUBLIC_URIS`에 추가합니다.

| 순서 | 규칙 | 설명 |
|---|------|------|
| 1 | `/api/auth/logout` → authenticated | 공개 경로(auth)의 예외 — 토큰 필수 |
| 2 | `/api/members/me/signup-complete` → hasRole(GUEST) | 소셜 추가정보 전용 |
| 3 | `PUBLIC_URIS` → permitAll | auth/oauth2 계열 + 각 도메인이 등록한 공개 경로 |
| 4 | `/api/members/**` → hasAnyRole(USER, ADMIN) | 마이페이지 계열 |
| 5 | `/api/admin/**` → hasRole(ADMIN) | **경로를 /api/admin 하위로 잡으면 자동 잠금** |
| 6 | `anyRequest()` → authenticated | 미분류 = 잠금 |

> ⚠️ `requestMatchers`는 **선언 순서대로** 매칭됩니다. 구체적 경로(1·2번)가 넓은 경로보다 먼저여야 하며, 순서를 바꾸면 규칙이 조용히 죽습니다.
>
> ⚠️ "조회(GET)는 공개, 작성(POST)은 인증"처럼 같은 경로에서 메서드별로 갈리면 경로 등록만으로 안 됩니다 — 회원 도메인 담당(지훈)에게 요청해 주세요.

---

## 4. [팀원 필독] 컨트롤러에서 로그인 회원 정보 꺼내기

JWT `subject`에는 **회원 PK(memberId)** 가 들어 있고, 필터가 claims만으로 `CustomUserDetails`(memberId, role)를 SecurityContext에 등록합니다.

### 방법 A — `@AuthenticationPrincipal` (권장)

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    Long memberId = userDetails.getMemberId();   // ← 회원 PK
    ...
}
```

### 방법 B — `SecurityUtil.getMemberId()` (서비스 계층 등)

### 사용 규칙

1. 컨트롤러는 방법 A 기본. 서비스 메서드는 `memberId`를 **파라미터로** 받도록 작성 (서비스가 SecurityContext에 직접 의존하면 테스트 불가).
2. 회원 엔티티가 필요하면 `memberRepository.findById(memberId).orElseThrow(...)`.
3. **요청 body나 파라미터로 memberId를 받지 마세요** — body의 memberId를 믿으면 타인 정보 조작이 가능합니다. 항상 토큰에서 추출합니다.
4. `CustomUserDetails`에는 email이 없습니다 (AT claims 최소화 설계). email이 필요하면 memberId로 조회하세요.

---

## 5. 토큰 정책 (확정)

| 항목 | Access Token | Refresh Token |
|------|--------------|---------------|
| 클라이언트 저장 | 전역 상태 변수 (localStorage/쿠키 금지) | HttpOnly 쿠키 |
| 서버 저장 | 없음 (로그아웃 jti 블랙리스트만) | Redis `refresh:{memberId}` |
| 전송 | `Authorization: Bearer` 헤더 | 쿠키 자동 전송 |
| 쿠키 Path | 해당 없음 | `/api/auth` (reissue/logout에만 전송) |
| 유효 기간 | 30분 | 14일 — **회전 시 TTL 갱신(슬라이딩)**, 14일 미접속 시 재로그인 |
| 재발급 시 | 새로 발급 | **로테이션** — 구 RT는 30초 grace 후 폐기 |
| claims | memberId(sub), role, category, jti | memberId(sub), category, jti |
| 세션 정책 | — | **단일 세션** — 새 기기 로그인 시 기존 세션 만료 |

**RT 쿠키 속성**: HttpOnly / SameSite=Lax / Secure(운영 true, 프로파일 분기) / Path=/api/auth

**전 세션 무효화가 일어나는 경우** (RT 삭제 → 이후 reissue 전부 401): 로그아웃, 비밀번호 변경, 비밀번호 재설정, 회원 탈퇴. 프론트는 이 경우 재로그인 유도가 필요합니다.

**grace period 상세**: 브라우저 재시작 등으로 여러 탭이 동시에 reissue하면 전부 구 RT를 들고 옵니다. 첫 요청이 회전시킨 뒤 구 RT를 `graceRefresh`(30초)에 보관해 나머지 요청을 흡수합니다. grace로 통과한 요청은 **재회전하지 않습니다** (회전 체인 방지). 재사용 탐지는 미적용 (grace와 상충, 데모 범위 외).

---

## 6. 프론트엔드 연동 가이드

AT가 상태 변수라 **새 탭·새로고침 시 사라집니다.** 필수 구현:

```js
axios.defaults.withCredentials = true;   // RT 쿠키 전송 필수
```

**① 부트스트랩** — 앱 마운트 시 `POST /api/auth/reissue` 1회 → 200이면 AT+회원정보로 로그인 복원, 401이면 비로그인. 완료 전 로딩 처리 필수(보호 라우트 진입 방지). 멀티탭 동시 마운트는 서버 grace가 흡수하므로 탭별 독립 부트스트랩으로 충분.

**② 401 인터셉터** — API 401 → reissue → 성공 시 원요청 1회 재시도 / 실패 시 상태 초기화 후 로그인 페이지. 무한 재시도 금지(1회 플래그), reissue 자체의 401은 인터셉터 제외.

**③ 필수 라우트 2개** — `/oauth/callback` (reissue 호출 + role 분기: GUEST→추가정보 / error 쿼리 처리), `/reset-password` (쿼리 token으로 재설정 폼).

**④ 흐름별 후처리 (협의 확정 사항)**

| 상황 | 프론트 처리 |
|------|------------|
| 추가정보 승급 성공 | **reissue 1회 호출** (role claim 갱신) 후 홈으로 |
| 비밀번호 변경/재설정 성공 | 상태 초기화 → 로그인 페이지 (전 세션 무효화됨) |
| 탈퇴 성공 | 상태 초기화 → 홈 (서버가 쿠키/세션 정리 완료) |
| signup에서 `AUTH_003` | "인증이 만료되었습니다" → 이메일 인증 단계로 복귀 (인증~제출 유효 30분) |
| `AUTH_106` (소셜 콜백 error) | "이미 가입된 이메일" → 일반 로그인 유도 |
| 닉네임 중복확인 | 형식 검증 통과 후에만 `GET /api/auth/nickname/available` 호출 (true=사용가능) |

**⑤ 금지** — AT를 localStorage/sessionStorage에 백업 금지. 소셜 버튼 axios 호출 금지(페이지 이동). RT 쿠키 직접 조작 금지(서버가 관리).

---

## 7. Redis 키 설계 (전체)

| 키 | TTL | 용도 |
|----|-----|------|
| `authCode:{email}` | 3분 | 이메일 인증 코드 |
| `verified:{email}` | 30분 | 인증 완료 ~ 가입 유예 (재발송 시 삭제) |
| `refresh:{memberId}` | 14일 | RT 대조 (회전 시 교체) |
| `graceRefresh:{memberId}` | 30초 | 동시 reissue 경합 흡수 |
| `blacklist:{jti}` | AT 잔여시간 | 로그아웃된 AT 차단 |
| `resetToken:{uuid}` | 30분 | 비밀번호 재설정 (일회용) |

- TTL은 RedisConfig가 아니라 **서비스 계층에서** 지정. 키 조작은 `RedisTokenService` 한 곳에만 존재 (인증코드/verified는 AuthService).
- 강제 로그아웃 = `refresh` + `graceRefresh` 삭제 (관리자 강제탈퇴 시 활용 예정).

---

## 8. 에러 응답 (전체)

공통 규격 `{ code, message, data: null }`. ([컨벤션 문서](./CONVENTIONS.md))

| code | HTTP | 상황 |
|------|------|------|
| `AUTH_001` | 409 | 이미 가입된 이메일 (인증코드 발송/가입) — 탈퇴 회원 이메일 포함 (재가입 불가 정책) |
| `AUTH_002` | 409 | 닉네임 중복 (가입/추가정보/프로필 수정 공통) |
| `AUTH_003` | 400 | 이메일 인증 미완료 (30분 초과 포함) |
| `AUTH_004` | 400 | 인증 코드 불일치/만료 |
| `AUTH_005` | 500 | 메일 전송 실패 |
| `AUTH_101` | 401 | 로그인 실패 (계정없음/비번틀림/탈퇴/소셜전용 — 구분 없음) |
| `AUTH_102` | 401 | 미인증 (토큰 없음/만료/위조/블랙리스트) — EntryPoint |
| `AUTH_103` | 403 | 권한 부족 (GUEST의 USER API 등) — AccessDeniedHandler |
| `AUTH_104` | 401 | RT 재발급 실패 → 재로그인 |
| `AUTH_106` | 409 | 소셜 이메일이 기존 계정과 충돌 (수동 연동 정책) |
| `AUTH_107` | 400 | 소셜 이메일 제공 미동의 |
| `AUTH_108` | 400 | 비밀번호 재설정 토큰 만료/무효 |
| `MEMBER_001` | 404 | 회원 없음 |
| `MEMBER_002` | 400 | 이미 추가정보 입력 완료 (USER의 승급 재시도) |
| `MEMBER_003` | 400 | 이미 탈퇴 처리된 회원 |
| `MEMBER_004` | 400 | 현재 비밀번호 불일치 (비밀번호 변경) |
| `MEMBER_005` | 400 | 소셜 전용 회원의 비밀번호 변경 시도 |
| `INVALID_INPUT` | 400 | @Valid 검증 실패 (message에 필드별 사유) |
| `DUPLICATE_CONFLICT` | 409 | 동시 요청으로 유니크 충돌 → 재시도 안내 |
| `NOT_FOUND` | 404 | 존재하지 않는 API 경로 |
| `METHOD_NOT_ALLOWED` | 405 | 허용되지 않은 HTTP 메서드 |
| `GLOBAL_ERROR` | 500 | 미처리 서버 오류 (서버 로그에 스택트레이스 기록됨) |

- ※ `AUTH_105`는 결번입니다.
- 401/403은 필터 레벨이라 GlobalExceptionHandler가 아닌 EntryPoint/AccessDeniedHandler가 응답합니다 (포맷은 동일).
- 비로그인 상태로 존재하지 않는 경로 호출 시 404가 아니라 **401이 먼저** 응답됩니다 (인가가 디스패처보다 앞 — 정상 동작).

---

## 부록 A. 설계 결정 기록

| 결정 | 내용 / 근거 |
|------|------------|
| 토큰 저장 방식 A | AT 상태변수 + RT 쿠키/Redis. 헤더 방식이라 CSRF 무관, 배포 도메인 제약 없음 |
| RT 로테이션 + grace 30초 | 탈취 RT 수명 단축. 참고 프로젝트가 grace 없이 회전만 적용해 멀티탭 경합 버그를 안고 있었음 — grace는 필수 세트 |
| 소셜 수동 연동 (AUTH_106) | 네이버의 이메일은 "연락처 이메일"로 계정 고유값이 아니며 위·변조 가능 → 이메일 기반 자동 연동은 계정 탈취 여지. 연동은 로그인 상태에서만(본인 증명) |
| 소셜 닉네임 미사용 | 추가정보 입력값만 저장 — 소셜 닉네임 유니크 충돌 문제 원천 차단 |
| 탈퇴 재가입 불가 | soft delete + uk_member_email 유지가 곧 정책. 별도 로직 없음 |
| 이메일 존재 비노출 | 로그인(AUTH_101 단일)·비번 재설정(항상 성공 응답)에서 일관 유지. 단 회원가입 email/send의 AUTH_001은 UX상 불가피한 노출. 이메일 중복확인 API는 미제공 |
| 인가 기본 잠금 | 미등록 API = authenticated. 등록 누락의 결과가 "개방 사고"가 아닌 "401 문의"가 되도록 안전한 방향으로 실패 |