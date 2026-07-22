package com.paw.ddasoom.common.security;

/**
 * 경로 등록제 상수 — 배열 2종(PUBLIC_URIS, USER_URIS).
 *
 * ⚠️ 팀원 규칙: 본인 도메인 경로는 이 파일에 "본인이 직접" 추가 + PR에 사유 한 줄.
 *
 * ┌─────────────────── 등록 치트시트 ───────────────────┐
 * │ 이 API는…                    → 등록 위치              │
 * │ 비로그인도 모든 메서드 가능     → PUBLIC_URIS (⚠️ 쓰기까지 열림) │
 * │ 로그인 회원(USER 이상)만       → USER_URIS ← 대부분 여기 │
 * │ 관리자 전용                   → /api/admin 하위로 잡으면 │
 * │                                자동 잠금 (등록 불필요)   │
 * │ 잘 모르겠음                    → 등록 안 함 (최소 로그인은 │
 * │                                강제됨) + 지훈 문의        │
 * └─────────────────────────────────────────────────┘
 *
 * 미등록 경로의 기본값 = authenticated (로그인만 검사, role 미검사).
 * "로그인만"으로 부족한 API(GUEST 차단 필요)는 반드시 USER_URIS에 등록할 것.
 *
 * ⚠️ "조회(GET)만 공개"는 이 파일의 경로 등록만으로는 불가능합니다.
 *    경로 단위 배열은 HTTP 메서드를 구분하지 않으므로 SecurityConfig에 메서드별 matcher가 필요합니다.
 *    해당 케이스가 생기면 회원 도메인 담당(지훈)에게 요청해 주세요. (과거 PUBLIC_GET_URIS 안내는
 *    실제 배열이 없어 혼란을 줘 제거 — 빈 배열은 permitAll 오작동 위험이 있어 신설하지 않았습니다.)
 */

public class SecurityConstants {

  public static final String[] PUBLIC_URIS = {
          // ── auth (회원/인증 — 지훈) ──
          // ⚠️ "/api/auth/**" 와일드카드 금지 — auth 하위에 새 API가 생기면 자동으로 공개되는 사고 방지.
          //    엔드포인트가 늘 때마다 여기 낱개로 추가한다. (logout은 미등록 → anyRequest에서 authenticated로 자연 잠금)
          "/api/auth/email/send",              // 인증코드 발송
          "/api/auth/email/verify",            // 인증코드 검증
          "/api/auth/signup",                  // 회원가입
          "/api/auth/login",                   // 일반 로그인
          "/api/auth/reissue",                 // 토큰 재발급 (RT 쿠키 기반 — AT 없이 호출됨)
          "/api/auth/password/reset-request",  // 비밀번호 재설정 메일 요청
          "/api/auth/password/reset",          // 비밀번호 재설정 실행
          "/api/auth/nickname/available",      // 닉네임 중복 확인 (가입 폼 실시간 검증)

          "/api/oauth2/**",            // 소셜 인가 시작
          "/api/login/oauth2/**",      // 소셜 콜백

          // ── Swagger / OpenAPI 문서 (로컬·개발용) ──
          // ⚠️ 운영 배포 시에는 프로파일 분리로 차단하거나 인증 뒤로 숨길 것 (문서 노출 = 공격 표면)
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/v3/api-docs/**",

          // ── 타 도메인 (각 담당자가 추가) ──
          // 예) "/api/animals",  "/api/animals/*",     — 유기동물 목록/상세 조회
          // 예) "/api/boards/**" GET 만 공개라면 아래 "메서드 분리" 참고 후 담당자와 협의
          // ── auth (공지사항/FAQ — 서진) ──
          "/api/notices",       // 공지사항 전체 목록 조회 (비로그인 허용)
          "/api/notices/*",     // 공지사항 상세 조회 (비로그인 허용)
          "/api/faqs",          // FAQ 전체 목록 조회
          "/api/faqs/*",         // FAQ 상세 목록 조회
          // ── Animal API (종식) ──
          // 목록/상세만 공개. likes(POST/DELETE)·nickname(PATCH)·me/likes(GET)는 더 깊은 경로라
          // 여기서 매칭되지 않아 인증 필수로 잠긴다. sync는 /api/admin/animals/sync로 이동해 ADMIN 자동 잠금.
          "/api/animals/list",            // 목록 조회 (공개)
          "/api/animals/*",               // 상세 조회 GET (공개) — 한 세그먼트만 매칭
  };

  /**
   * 비로그인도 "조회(GET)만" 허용 — 쓰기(POST/PATCH/DELETE)는 같은 경로라도 잠긴다.
   * SecurityConfig에서 {@code HttpMethod.GET}과 함께 매칭되므로, 여기 등록한 경로의
   * 쓰기 요청은 뒤의 USER_URIS 규칙으로 자연히 떨어진다.
   *
   * ⚠️ 개인화 조회 경로(/my 등)를 와일드카드로 삼키지 않도록 주의할 것.
   *    (예: "/api/posts/*" 는 "/api/posts/my" 도 매칭 — SecurityConfig에서 /my를 먼저 잠가둠)
   */
  public static final String[] PUBLIC_GET_URIS = {
          // ── 게시판 (창호) ──
          // 커뮤니티 열람은 비로그인 공개, 작성/수정/삭제는 USER 전용.
          "/api/posts",                // 목록 조회 (boardType 쿼리 파라미터로 3개 보드 공용)
          "/api/posts/*",              // 상세 조회 — 한 세그먼트만 매칭 (/api/posts/{postId})
          "/api/posts/*/comments",     // 댓글 목록 조회
  };

  /**
   * USER/ADMIN 전용 — GUEST(소셜 가입 후 추가정보 미입력, nickname=NULL) 차단.
   * "로그인 필요 + GUEST는 안 됨"인 쓰기/개인화 API는 전부 여기로.
   */
  public static final String[] USER_URIS = {
          // ── QnA/신고 (서진) ──
          "/api/qnas/**",      // 1:1 문의 (본인 문의 작성/조회 — GUEST 차단)
          "/api/reports/**",   // 신고 (도메인 선등록 — 컨트롤러 구현 시 @RequestMapping 값과 일치 재확인)

          // ── 게시판 (창호) ──
          // 조회(GET 목록/상세/댓글목록)는 PUBLIC_GET_URIS로 공개, 그 외 메서드는 여기서 USER 전용으로 잠근다.
          // SecurityConfig에서 GET permitAll 규칙이 이 규칙보다 "먼저" 선언돼 있어야 성립 — 순서 변경 금지.
          "/api/posts/**",     // 작성/수정/삭제 + 개인화 조회(/my) — GUEST 차단

          // ── 이미지 업로드 (창호) ──
          // 업로드 주체는 게시글/문의 작성자와 동일 → 게시판과 같은 정책. ADMIN(공지 이미지)도 통과.
          "/api/images",       // 업로드 (POST) — GUEST 차단

          // ── 동물 좋아요 (종식) — 본인 확인 후 추가 ──
          "/api/animals/*/likes",

          // ── 임보 (경우) — 본인 확인 후 추가 ──
          "/api/fosters/**",
  };
}