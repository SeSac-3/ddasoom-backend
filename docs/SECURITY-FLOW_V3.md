# 🔐 따숨 Security Flow

> 인증/인가가 어떻게 동작하는지, 그리고 **각 도메인 개발자가 컨트롤러에서 로그인한 회원 정보를 어떻게 꺼내 쓰는지** 안내하는 문서입니다.
>
> ✅ **구현 상태**: 이메일 인증 회원가입(시도 제한·재발송 쿨다운·IP 발송 제한 포함), 일반 로그인/로그아웃, 토큰 재발급(RT 로테이션 + grace), SNS 로그인(카카오/네이버/구글, 탈퇴·제재 회원 차단), GUEST 추가정보 승급, 마이페이지(조회/수정/비밀번호 변경/탈퇴), 비밀번호 재설정(탈퇴 회원 차단), 관리자 회원 관리(목록/상세/강제탈퇴/복구/**상태 제재**), 관리자 대시보드·통계, **Swagger API 문서** — 전부 구현 완료.
>
> 🔑 **토큰 정책 (확정)**:
> - **Access Token** → 서버 미저장(로그아웃 jti 블랙리스트만 예외), 클라이언트는 **전역 상태 변수**(zustand) 보관, 매 요청 `Authorization: Bearer` 헤더. **localStorage / sessionStorage / 쿠키 저장 금지.**
> - **Refresh Token** → **HttpOnly 쿠키** + **Redis** 이중 관리. 프론트 JS 접근 불가.
> - **RT 로테이션 적용**, 멀티탭 동시 재발급 경합은 **서버 grace period(30초)** 가 흡수.
> - **탈퇴(자진·강제)·제재(HIDDEN) 시 회원 단위 즉시 차단** — 재발급 차단뿐 아니라 이미 발급된 모든 AT를 `forceLogout` 마커로 즉시 무효화. (5절 참고)

---

## 1. 권한(Role) 체계

| Role | 대상 | 접근 범위 |
|------|------|-----------|
| `GUEST` | SNS 가입 후 **추가정보 미입력** 회원 | 추가정보 입력 API만 |
| `USER` | 일반 가입 완료, SNS 가입 + 추가정보 완료 | 일반 서비스 전체 |
| `ADMIN` | 시드 데이터로만 생성 (`db/migration/V4__seed_admin.sql`, 비밀번호 `Ddasoom1!`) | `/api/admin/**` 포함 전체 |

- 일반 회원가입 → 즉시 `USER`. SNS 가입 → `GUEST`로 시작, 추가정보 입력 시 `USER` 승급.
- **ADMIN은 회원가입 API로 만들 수 없습니다.**

### 회원 상태(MemberStatus) — Role과 직교하는 별도 축

| Status | 의미 | 로그인 |
|--------|------|--------|
| `ACTIVE` | 정상 | 가능 |
| `HIDDEN` | 신고 제재로 숨김 처리 | **불가 (즉시 차단)** |

- Role이 "무엇을 할 수 있는가"라면, Status는 "활동 자격이 있는가"입니다. 둘은 독립적으로 판정됩니다.
- 탈퇴(`deletedAt`)까지 포함하면 회원 상태는 **활성 / 숨김 / 탈퇴** 3분류로 표시됩니다(관리자 회원 목록 기준).
- HIDDEN 전환 경로는 두 가지 — 관리자 수동(`PATCH /api/admin/members/{id}/status`), 신고 승인 자동(report 도메인 → `hideMember()`). **두 경로 모두 세션까지 즉시 차단**합니다(5절).

---

## 2. 전체 인증 흐름 (구현 완료 기준)

```
[일반 회원가입]
POST /api/auth/email/send    인증코드 발송 (재발송 겸용 — 기존 인증상태 무효화)
                             ├ 동일 이메일 60초 쿨다운 (AUTH_006)
                             └ 동일 IP 시간당 10회 제한 (AUTH_007) — 이메일을 바꿔가며 하는 대량 발송 방어
POST /api/auth/email/verify  코드 검증 → verified:{email} (30분) — 시도 5회 초과 시 코드 폐기
POST /api/auth/signup        verified 확인 → BCrypt 저장(USER) → 201 + 환영 메일

[일반 로그인]
POST /api/auth/login
  → ① 비밀번호 먼저 검증
       계정없음 / 비번틀림 / 소셜전용(비번 null) → 전부 AUTH_101 하나 (열거 공격 방지)
  → ② 비번이 맞은 경우에만 계정 상태 검사 (사유를 구분해 안내)
       탈퇴 → AUTH_109 / 제재(HIDDEN) → AUTH_110
  → body: AT + expiresIn(초) + 회원요약  /  Set-Cookie: RT (HttpOnly)
  → Redis refresh:{memberId} 저장 + login_log 기록

  ※ 검증 순서가 핵심입니다. 비밀번호를 맞히지 못한 요청에는 상태 사유가 절대 노출되지 않으므로,
    이메일만으로 "이 계정이 탈퇴/제재 상태인가"를 알아내는 열거 공격이 성립하지 않습니다.
    반대로 비번을 아는 본인에게는 "왜 로그인이 안 되는지"를 정확히 안내합니다. (부록 A 참고)

[SNS 로그인 — 카카오/네이버/구글]
소셜 버튼 = <a href="/api/oauth2/authorization/{provider}">  ※ axios 금지, 페이지 이동
  → provider 동의 → 콜백(/api/login/oauth2/code/{provider})
  → provider+providerId 로 회원 판별 (신원은 이메일이 아님!)
      ├ 기존 연동 회원 → 로그인 (탈퇴 → AUTH_109 / 제재 → AUTH_110)
      ├ 신규 + 이메일 충돌 → AUTH_106 (수동 연동 정책 — 자동 연동 금지)
      └ 신규 → Member(GUEST, password=NULL) + MemberSocial 생성
  → 성공: RT 쿠키만 발급 후 {프론트}/oauth/callback 리다이렉트 (AT는 콜백의 reissue가 발급)
  → 실패: {프론트}/oauth/callback?error=AUTH_1xx → 프론트가 /login?error=코드 로 넘겨 배너 안내

[인증 API 호출]
Authorization: Bearer {AT}
  → AuthJwtTokenFilter: 파싱 → category=access 확인 → jti 블랙리스트 확인 → memberId 강제로그아웃 확인
  → claims만으로 CustomUserDetails 구성 (매 요청 DB 조회 없음, Redis 조회 2회)
  ※ 권한 변경(GUEST→USER 등)은 reissue 후 새 AT부터 반영됨
  ※ Redis 장애 시 fail-close — 인증 미설정으로 통과시켜 보호 API는 401 (강제 로그아웃이 장애를 틈타 뚫리지 않도록)

[재발급 — RT 로테이션 + grace 30초]
POST /api/auth/reissue  (RT 쿠키 자동 전송)
  → 탈퇴·제재(HIDDEN) 회원이면 즉시 차단 (사유 구분 없이 AUTH_104 — 아래 ※)
  → Redis 주 키 대조 일치 → 새 AT + 새 RT(회전), 구 RT는 graceRefresh(30초)로
  → 주 키 불일치 → grace 키 대조 → 일치 시 새 AT만 (재회전 금지)
  → 둘 다 불일치 → 401 AUTH_104
  ※ reissue는 부트스트랩·401 인터셉터가 자동 호출하는 경로라 프론트가 "재로그인 유도" 하나로만
    대응하면 됩니다. 사유 안내는 사용자가 의도적으로 시도하는 로그인 시점(AUTH_109/110)에 합니다.

[로그아웃]
POST /api/auth/logout  (미등록 경로 → anyRequest에서 authenticated로 자연 잠금)
  → refresh + grace 삭제, AT jti 블랙리스트, RT 쿠키 삭제

[회원 탈퇴 — 자진·강제 공용]
DELETE /api/members/me  (본인)  /  DELETE /api/admin/members/{memberId}  (관리자, ADMIN 계정 대상 불가)
  → soft delete + refresh/grace 삭제 + forceLogout 마커 등록(AT 최대 수명만큼)
  → ⚠️ 재발급 차단뿐 아니라 다른 탭·기기의 기발급 AT도 즉시 차단됨 (5절 참고)

[회원 제재 — 상태 변경(HIDDEN)]
PATCH /api/admin/members/{memberId}/status   (관리자 수동, ADMIN 계정 대상 불가)
report 도메인의 신고 승인 → AdminMemberService.hideMember()  (자동)
  → status=HIDDEN + refresh/grace 삭제 + forceLogout 마커 등록  ← 탈퇴와 동일 세트
  → ACTIVE 복귀 시 마커 해제 (대칭)

[관리자 계정 복구]
PATCH /api/admin/members/{memberId}/restore
  → deletedAt=null + forceLogout 마커 해제 (DB·Redis 양쪽을 되돌리는 온전한 역연산)

[GUEST 추가정보 → USER 승급]
PATCH /api/members/me/signup-complete  (hasRole GUEST)
  → 닉네임 중복 검사 → updateExtraInfo → USER 승급
  → ⚠️ 프론트: 성공 후 reissue 1회 필수 (AT의 role claim 갱신)

[비밀번호 재설정(찾기)]
POST /api/auth/password/reset-request  { email }
  → 이메일 존재 여부와 무관하게 항상 동일 성공 응답 (열거 공격 방지)
  → 대상일 때만 메일 발송: {프론트}/reset-password?token={uuid}  (Redis 30분)
POST /api/auth/password/reset  { token, newPassword }
  → 일회용 토큰 검증 → 탈퇴 회원 여부 확인(탈퇴 시 무효 토큰과 동일 응답) → 변경 → 전 세션 무효화(재로그인 필요)
```

---

## 3. 인가 규칙 (확정 — 기본 잠금 + 등록제)

**원칙: 등록되지 않은 API는 토큰 필수가 기본값입니다.** 공개가 필요하면 담당자가 직접 `SecurityConstants`에 추가합니다.

| 순서 | 규칙 | 설명 |
|---|------|------|
| 1 | `/api/members/me/signup-complete` → hasRole(GUEST) | 소셜 추가정보 전용 — members 규칙보다 먼저 |
| 2 | `PUBLIC_URIS` → permitAll | auth 낱개 등록 + oauth2 계열 + Swagger + 각 도메인 공개 경로 |
| 3 | `/api/members/**` → hasAnyRole(USER, ADMIN) | 마이페이지 계열 |
| 4 | `USER_URIS` → hasAnyRole(USER, ADMIN) | 로그인 회원 전용 등록 경로 (**GUEST 차단**) |
| 5 | `/api/admin/**` → hasRole(ADMIN) | **경로를 /api/admin 하위로 잡으면 자동 잠금** |
| 6 | `anyRequest()` → authenticated | 미분류 = 잠금 |

> ⚠️ `requestMatchers`는 **선언 순서대로** 매칭됩니다. 구체적 경로(1·2번)가 넓은 경로보다 먼저여야 하며, 순서를 바꾸면 규칙이 조용히 죽습니다.
>
> ⚠️ **`SecurityConstants`의 배열은 2종(PUBLIC_URIS / USER_URIS)입니다.** "조회(GET)만 공개"는 경로 등록만으로 불가능합니다(배열이 HTTP 메서드를 구분하지 않음). 해당 케이스는 SecurityConfig에 메서드별 matcher가 필요하니 회원 도메인 담당(지훈)에게 요청해 주세요.
>
> ⚠️ `authenticated`는 **로그인 여부만** 검사합니다(role 미검사). GUEST를 막아야 하는 API는 반드시 `USER_URIS`에 등록하세요.
>
> ⚠️ **Swagger 경로**(`/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`)는 로컬·개발 편의를 위해 공개 상태입니다. **운영 배포 시 프로파일 분리로 차단하거나 인증 뒤로 숨겨야 합니다**(문서 노출 = 공격 표면).

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
5. **`SecurityContextHolder`는 ThreadLocal 기반입니다.** `@Async` 등 별도 스레드에서 `SecurityUtil.getMemberId()`를 호출하지 마세요 — 새 스레드에는 컨텍스트가 없어 즉시 인증 예외가 발생합니다. 비동기 로직이 필요하면 부모 스레드에서 memberId를 추출해 **파라미터로 전달**하세요.
6. `HttpServletRequest`에서만 얻는 값(clientIp 등)도 **컨트롤러가 추출해 서비스에 전달**합니다 (서비스가 서블릿 API에 의존하지 않도록).

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

**전 세션 무효화가 일어나는 경우** (RT 삭제 → 이후 reissue 전부 401): 로그아웃, 비밀번호 변경, 비밀번호 재설정, 회원 탈퇴, **회원 제재(HIDDEN)**. 프론트는 이 경우 재로그인 유도가 필요합니다.

**grace period 상세**: 브라우저 재시작 등으로 여러 탭이 동시에 reissue하면 전부 구 RT를 들고 옵니다. 첫 요청이 회전시킨 뒤 구 RT를 `graceRefresh`(30초)에 보관해 나머지 요청을 흡수합니다. grace로 통과한 요청은 **재회전하지 않습니다** (회전 체인 방지). 재사용 탐지는 미적용 (grace와 상충, 데모 범위 외).

**강제 로그아웃 (`forceLogout` 마커) — 탈퇴·제재 공용, 회원 단위 즉시 차단**:
jti 블랙리스트는 "이 토큰은 무효"라는 **토큰 단위** 차단입니다. 그런데 관리자가 제3자를 강제탈퇴·제재하는 경우, 서버는 그 대상 회원이 지금 어떤 AT를 들고 있는지 알 방법이 없어(브라우저 상태 변수에만 존재) 토큰 단위 차단이 원천적으로 불가능합니다. 이를 위해 **회원 단위** 차단 키 `forceLogout:{memberId}`(TTL = AT 최대 수명, 30분)를 사용합니다.

- **등록 시점**:
  - `withdraw()` — 자진 탈퇴·관리자 강제탈퇴 공용
  - `changeStatus(HIDDEN)` / `hideMember()` — 관리자 수동 제재·신고 승인 자동 제재 공용
- **검사 시점**: `AuthJwtTokenFilter`가 매 요청마다 jti 블랙리스트 확인 직후 memberId 기준으로 확인합니다. 걸리면 인증 미설정 상태로 통과 → 401.
- **해제 시점**: 관리자 복구(`restore()`) 시, 그리고 상태를 ACTIVE로 되돌릴 때. DB와 Redis 양쪽을 온전히 되돌리는 하나의 역연산이 되도록 설계했습니다.
- **트레이드오프**: 이 검사로 인증된 요청당 Redis 조회가 2회(jti 블랙리스트 + memberId 마커)로 늘었습니다. AT의 완전한 무상태 원칙을 일부 양보한 것이지만, "정지된 회원은 즉시 아무것도 할 수 없어야 한다"는 요구를 충족하기 위한 의도적 선택입니다. 제재까지 범위를 넓혔지만 **검사 지점이 동일해 성능 증분은 0**입니다. 실행 중인 요청 1건이 차단 직후에 완료되는 것까지는 막지 않습니다(서블릿 스레드 모델의 경계 — 필터의 목적은 "진입 차단"이지 "실행 중단"이 아닙니다).
- **비밀번호 변경/재설정에는 미적용**: 그 경우의 "다른 탭 AT 최대 30분 잔존"은 RT 무효화(재발급 차단) + 단일 세션 정책 + AT 자연 만료로 설명 가능한 트레이드오프이며, 탈퇴·제재처럼 "활동 자격 자체가 소멸"하는 질적으로 다른 문제가 아니기 때문입니다.

---

## 6. 프론트엔드 연동 가이드

AT가 상태 변수라 **새 탭·새로고침 시 사라집니다.** 필수 구현:

```js
axios.defaults.withCredentials = true;   // RT 쿠키 전송 필수
```

**① 부트스트랩** — 앱 마운트 시 `POST /api/auth/reissue` 1회 → 200이면 AT+회원정보로 로그인 복원, 401이면 비로그인. 완료 전 로딩 처리 필수(보호 라우트 진입 방지). 멀티탭 동시 마운트는 서버 grace가 흡수하므로 탭별 독립 부트스트랩으로 충분.

**② 401 인터셉터** — API 401 → reissue → 성공 시 원요청 1회 재시도 / 실패 시 상태 초기화 후 로그인 페이지. 무한 재시도 금지(1회 플래그), reissue 자체의 401은 인터셉터 제외. **403은 재시도 대상이 아닙니다** — 즉시 안내 화면(`/forbidden`)으로 이동.
  - ⚠️ **단 로그인 요청의 403은 예외**입니다. 탈퇴(AUTH_109)·제재(AUTH_110)는 403으로 내려가므로, 인터셉터가 `/forbidden`으로 보내버리면 로그인 화면에서 사유 배너를 띄울 수 없습니다. 로그인 요청은 리다이렉트에서 제외하고 호출부 catch로 넘기세요.

**③ 필수 라우트 2개** — `/oauth/callback` (reissue 호출 + role 분기: GUEST→추가정보 / error 쿼리 처리), `/reset-password` (쿼리 token으로 재설정 폼).

**④ 흐름별 후처리 (협의 확정 사항)**

| 상황 | 프론트 처리 |
|------|------------|
| 추가정보 승급 성공 | **reissue 1회 호출** (role claim 갱신) 후 홈으로 |
| 비밀번호 변경/재설정 성공 | 상태 초기화 → 로그인 페이지 (전 세션 무효화됨) |
| 탈퇴 성공 | 상태 초기화 → 홈 (서버가 쿠키/세션 정리 완료) |
| signup에서 `AUTH_003` | "인증이 만료되었습니다" → 이메일 인증 단계로 복귀 (인증~제출 유효 30분) |
| email/send에서 `AUTH_006` | "1분에 한 번만 발송 가능" 안내 (재발송 쿨다운) |
| email/send에서 `AUTH_007` | "잠시 후 다시 시도" 안내 (IP 시간당 발송 제한) |
| 로그인에서 `AUTH_109` | "탈퇴 처리된 계정" 배너 → 1:1 문의 안내 |
| 로그인에서 `AUTH_110` | "이용이 제한된 계정" 배너 → 1:1 문의 안내 |
| `AUTH_106` (소셜 콜백 error) | "이미 가입된 이메일" → 일반 로그인 유도 |
| 닉네임 중복확인 | 형식 검증 통과 후에만 `GET /api/auth/nickname/available` 호출 (true=사용가능) |

**⑤ 금지** — AT를 localStorage/sessionStorage에 백업 금지. 소셜 버튼 axios 호출 금지(페이지 이동). RT 쿠키 직접 조작 금지(서버가 관리). **AT를 콘솔에 출력하지 말 것**(공유 PC·화면 녹화·원격 지원에서 노출 — 백엔드 로깅 규칙과 대칭).

---

## 7. Redis 키 설계 (전체)

| 키 | TTL | 용도 |
|----|-----|------|
| `authCode:{email}` | 3분 | 이메일 인증 코드 |
| `authCodeAttempt:{email}` | 3분 (코드와 동일) | 인증코드 검증 실패 횟수 — 5회 초과 시 코드 폐기 |
| `authCodeCooldown:{email}` | 60초 | 인증 메일 재발송 쿨다운 |
| `authCodeSendIp:{ip}` | 1시간 | IP 단위 발송 횟수 — 시간당 10회 초과 시 AUTH_007 |
| `verified:{email}` | 30분 | 인증 완료 ~ 가입 유예 (재발송 시 삭제) |
| `refresh:{memberId}` | 14일 | RT 대조 (회전 시 교체) |
| `graceRefresh:{memberId}` | 30초 | 동시 reissue 경합 흡수 |
| `blacklist:{jti}` | AT 잔여시간 | 로그아웃된 AT 차단 (토큰 단위) |
| `forceLogout:{memberId}` | AT 최대 수명(30분) | 탈퇴·제재 회원의 **기발급 AT 전부** 즉시 차단 (회원 단위). 복구/ACTIVE 복귀 시 삭제 |
| `resetToken:{uuid}` | 30분 | 비밀번호 재설정 (일회용) |

- TTL은 RedisConfig가 아니라 **서비스 계층에서** 지정. 키 조작은 `RedisTokenService` 한 곳에만 존재 (인증코드/verified/attempt/cooldown/IP제한은 AuthService).

---

## 8. 에러 응답 (전체)

공통 규격 `{ code, message, data: null }`. ([컨벤션 문서](./BACKEND_CODE_CONVENTIONS.md))

| code | HTTP | 상황 |
|------|------|------|
| `AUTH_001` | 409 | 이미 가입된 이메일 (인증코드 발송/가입) — 탈퇴 회원 이메일 포함 (재가입 불가 정책) |
| `AUTH_002` | 409 | 닉네임 중복 (가입/추가정보/프로필 수정 공통) |
| `AUTH_003` | 400 | 이메일 인증 미완료 (30분 초과 포함) |
| `AUTH_004` | 400 | 인증 코드 불일치/만료 (5회 초과로 코드가 폐기된 경우 포함) |
| `AUTH_005` | 500 | 메일 전송 실패 |
| `AUTH_006` | 429 | 인증 메일 재발송 쿨다운(60초) 이내 재요청 |
| `AUTH_007` | 429 | IP 단위 인증 메일 발송 제한(시간당 10회) 초과 |
| `AUTH_101` | 401 | 로그인 실패 — 계정없음/비번틀림/소셜전용 (구분 없음) |
| `AUTH_102` | 401 | 미인증 (토큰 없음/만료/위조/블랙리스트/강제로그아웃) — EntryPoint |
| `AUTH_103` | 403 | 권한 부족 (GUEST의 USER API 등) — AccessDeniedHandler |
| `AUTH_104` | 401 | RT 재발급 실패 (불일치/만료/탈퇴/제재 회원) → 재로그인 |
| `AUTH_106` | 409 | 소셜 이메일이 기존 계정과 충돌 (수동 연동 정책) |
| `AUTH_107` | 400 | 소셜 이메일 제공 미동의 |
| `AUTH_108` | 400 | 비밀번호 재설정 토큰 만료/무효 (탈퇴 회원의 토큰 사용 포함) |
| `AUTH_109` | 403 | **탈퇴 처리된 계정** — 일반 로그인(비번 일치 시) / 소셜 로그인 |
| `AUTH_110` | 403 | **제재(HIDDEN) 계정** — 일반 로그인(비번 일치 시) / 소셜 로그인 |
| `MEMBER_001` | 404 | 회원 없음 |
| `MEMBER_002` | 400 | 이미 추가정보 입력 완료 (USER의 승급 재시도) |
| `MEMBER_003` | 400 | 이미 탈퇴 처리된 회원 |
| `MEMBER_004` | 400 | 현재 비밀번호 불일치 (비밀번호 변경) |
| `MEMBER_005` | 400 | 소셜 전용 회원의 비밀번호 변경 시도 |
| `MEMBER_006` | 400 | 탈퇴 상태가 아닌 회원의 복구 시도 |
| `MEMBER_008` | 409 | ADMIN 계정의 상태 변경 시도 (제재 대상 불가) |
| `INVALID_INPUT` | 400 | @Valid 검증 실패 / 타입 불일치 / 필수 파라미터 누락 (message에 사유) |
| `DUPLICATE_CONFLICT` | 409 | 동시 요청으로 유니크 충돌 → 재시도 안내 |
| `NOT_FOUND` | 404 | 존재하지 않는 API 경로 |
| `METHOD_NOT_ALLOWED` | 405 | 허용되지 않은 HTTP 메서드 |
| `GLOBAL_ERROR` | 500 | 미처리 서버 오류 (서버 로그에 스택트레이스 기록됨) |

- ※ `AUTH_105`는 결번입니다.
- 401/403은 필터 레벨이라 GlobalExceptionHandler가 아닌 EntryPoint/AccessDeniedHandler가 응답합니다 (포맷은 동일).
- 비로그인 상태로 존재하지 않는 경로 호출 시 404가 아니라 **401이 먼저** 응답됩니다 (인가가 디스패처보다 앞 — 정상 동작).

---

## 9. 알려진 한계와 의도적으로 보류한 항목

아래는 "몰라서 안 한 것"이 아니라 **인지한 뒤 현 단계(부트캠프 팀 프로젝트, 데모 규모)에서 도입하지 않기로 결정한 항목**입니다.

- 로그인 응답의 타이밍 사이드채널(이메일 존재 여부 추정 가능성)
- reissue 회전의 완전 동시 요청 시 원자성 (grace가 대부분의 실사용 케이스를 흡수)
- `forceLogout`의 이진 마커 방식 (iat 비교 방식이 상위 호환이나 현 요구사항은 키 삭제로 충분)
- 일반 로그아웃 시 다른 탭/기기 AT의 즉시 차단 (탈퇴·제재와 달리 RT 무효화 + 자연 만료로 설명 가능한 트레이드오프)
- 로그인 시도 횟수 제한 (BCrypt 연산 비용이 1차 완화 역할)
- Soft delete 회원의 개인정보 완전 파기 (즉시 마스킹은 계정 복구 기능과 양립 불가 — 유예 후 배치 파기가 정답)
- yml 프로파일 분리(local/prod) — `show-sql`, hibernate trace 로그, `cookie-secure=false`가 단일 yml에 있음. **배포 전 필수 과제**
- Swagger 경로의 운영 차단 — **배포 전 필수 과제**

**배포 시 결정 필요**: RT 쿠키의 `SameSite=Lax`는 프론트/백엔드가 same-site로 배포되는 것을 전제로 CSRF 방어를 겸합니다. 완전히 다른 도메인으로 배포한다면 쿠키 정책과 CSRF 대책을 별도로 재설계해야 합니다.

---

## 부록 A. 설계 결정 기록

| 결정 | 내용 / 근거 |
|------|------------|
| 토큰 저장 방식 A | AT 상태변수 + RT 쿠키/Redis. 헤더 방식이라 CSRF 무관, 배포 도메인 제약 없음 |
| RT 로테이션 + grace 30초 | 탈취 RT 수명 단축. 참고 프로젝트가 grace 없이 회전만 적용해 멀티탭 경합 버그를 안고 있었음 — grace는 필수 세트 |
| 강제 로그아웃 마커 (탈퇴·제재 공용) | jti 블랙리스트(토큰 단위)로는 "서버가 대상의 현재 AT를 모르는" 제3자 조치 상황을 막을 수 없어 회원 단위 마커를 병행. "정지된 회원은 즉시 아무것도 할 수 없어야 한다"는 요구 충족이 AT 무상태 원칙보다 우선 |
| **HIDDEN 제재 = 탈퇴 동급 즉시 차단** | 상태 컬럼만 바꾸면 ① 기존 세션이 계속 활동 ② RT 슬라이딩 갱신으로 사실상 무기한 유지 ③ 소셜 계정은 재로그인까지 가능 — "제재가 로그인 폼 하나만 잠그는" 상태가 됨. 탈퇴에 세운 원칙을 제재에도 동일 적용하기로 결정(기존 인프라 재사용이라 성능 증분 0). 검사 지점: 일반 로그인·소셜 로그인·reissue·매 요청 필터 |
| **로그인 실패 응답 2단 분리 (비번 → 상태)** | "탈퇴/제재 사유 안내(UX)"와 "이메일 존재 노출 방지(보안)"가 상충. 비밀번호가 일치할 때만 사유를 노출해 절충 — 공격자는 비번까지 맞혀야 사유를 볼 수 있어 열거 공격 난이도가 유지되고, 정당한 본인은 정확한 안내를 받음 |
| reissue의 사유 미구분 | 부트스트랩·인터셉터가 자동 호출하는 경로라 프론트는 "재로그인 유도" 하나로 대응하면 충분. 사유 안내는 사용자가 의도적으로 시도하는 로그인 시점에 |
| 소셜 수동 연동 (AUTH_106) | 네이버의 이메일은 "연락처 이메일"로 계정 고유값이 아니며 위·변조 가능 → 이메일 기반 자동 연동은 계정 탈취 여지. 연동은 로그인 상태에서만(본인 증명) |
| 소셜 닉네임 미사용 | 추가정보 입력값만 저장 — 소셜 닉네임 유니크 충돌 문제 원천 차단 |
| 탈퇴 재가입 불가 | soft delete + uk_member_email 유지가 곧 정책. 별도 로직 없음 |
| 이메일 존재 비노출 | 로그인(AUTH_101 단일)·비번 재설정(항상 성공 응답)에서 일관 유지. 단 회원가입 email/send의 AUTH_001은 UX상 불가피한 노출. 이메일 중복확인 API는 미제공 |
| IP 단위 발송 제한 (AUTH_007) | 이메일 단위 쿨다운(60초)만으로는 "이메일을 바꿔가며" 하는 대량 발송을 못 막음. 신규 코드로 분리한 이유는 기존 AUTH_006의 프론트 문구("1분에 한 번")와 의미가 달라서 |
| 인가 기본 잠금 | 미등록 API = authenticated. 등록 누락의 결과가 "개방 사고"가 아닌 "401 문의"가 되도록 안전한 방향으로 실패 |
| 탈퇴/제재 상태의 노출 비대칭 | 일반 로그인은 비번 일치 시에만, 비밀번호 재설정은 항상 비노출, 소셜은 provider 본인 인증 통과자에게만 노출 — "누구에게 보이는 응답인가"로 노출 수준을 결정 |
| Redis 장애 시 fail-close | Redis가 죽으면 로그인·재발급 자체가 불가하므로 fail-open의 실익이 없고, 강제 로그아웃(치안 기능)이 장애를 틈타 무력화되지 않아야 함 |
