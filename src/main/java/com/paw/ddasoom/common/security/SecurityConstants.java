package com.paw.ddasoom.common.security;

// ⚠️ 팀원 규칙: 본인 도메인의 공개 경로는 이 배열에 "본인이 직접" 추가 + PR에 사유 한 줄
public class SecurityConstants {

  public static final String[] PUBLIC_URIS = {
      // ── auth (회원/인증 — 지훈) ──
      "/api/auth/**",              // 가입·로그인·재발급·비번재설정 (logout은 SecurityConfig에서 예외 처리)
      "/api/oauth2/**",            // 소셜 인가 시작
      "/api/login/oauth2/**",      // 소셜 콜백

      // ── 타 도메인 (각 담당자가 추가) ──
      // 예) "/api/animals",  "/api/animals/*",     — 유기동물 목록/상세 조회 
      // 예) "/api/boards/**" GET 만 공개라면 아래 "메서드 분리" 참고 후 담당자와 협의
  };
}
