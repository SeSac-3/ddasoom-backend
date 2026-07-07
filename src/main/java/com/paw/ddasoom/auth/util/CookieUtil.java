package com.paw.ddasoom.auth.util;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CookieUtil {
  public static final String REFRESH_COOKIE_NAME = "refreshToken";

  // RT가 필요한 곳(reissue/logout) 외 전송 차단
  private static final String REFRESH_COOKIE_PATH = "/api/auth";

  @Value("${ddasoom.cookie-secure:false}")  // 운영 true (HTTPS 전용)
  private boolean cookieSecure;

  /** RT 쿠키 생성 — HttpOnly + SameSite=Lax (jakarta Cookie는 SameSite 미지원이라 ResponseCookie 사용) */
  public ResponseCookie createRefreshCookie(String refreshToken, Duration maxAge) {
      return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
              .httpOnly(true)
              .secure(cookieSecure)
              .sameSite("Lax")
              .path(REFRESH_COOKIE_PATH)
              .maxAge(maxAge)
              .build();
  }

  /** RT 쿠키 삭제 (로그아웃) — Max-Age=0 */
  public ResponseCookie deleteRefreshCookie() {
      return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
              .httpOnly(true)
              .secure(cookieSecure)
              .sameSite("Lax")
              .path(REFRESH_COOKIE_PATH)
              .maxAge(0)
              .build();
  }

  /** 요청 쿠키에서 RT 추출 — 없을 수 있으므로 Optional (컨벤션: find = Optional) */
  public Optional<String> findRefreshToken(HttpServletRequest request) {
      if (request.getCookies() == null) {
          return Optional.empty();
      }
      return Arrays.stream(request.getCookies())
              .filter(cookie -> REFRESH_COOKIE_NAME.equals(cookie.getName()))
              .map(Cookie::getValue)
              .findFirst();
  }
}
