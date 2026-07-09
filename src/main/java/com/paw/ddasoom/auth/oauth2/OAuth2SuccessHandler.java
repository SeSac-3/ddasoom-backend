package com.paw.ddasoom.auth.oauth2;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.paw.ddasoom.auth.domain.LoginLog;
import com.paw.ddasoom.auth.domain.LoginType;
import com.paw.ddasoom.auth.repository.LoginLogRepository;
import com.paw.ddasoom.auth.service.RedisTokenService;
import com.paw.ddasoom.auth.util.CookieUtil;
import com.paw.ddasoom.auth.util.JwtUtil;
import com.paw.ddasoom.member.repository.MemberRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 소셜 로그인 성공 처리 — 방식 ⓑ: AT를 만들지 않는다.
 * RT만 발급(Redis + HttpOnly 쿠키)하고 프론트 콜백으로 리다이렉트하면,
 * 콜백 페이지의 reissue가 AT를 발급한다 (일반 로그인과 이후 흐름 통일).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler{

  private final JwtUtil jwtUtil;
  private final RedisTokenService redisTokenService;
  private final CookieUtil cookieUtil;
  private final MemberRepository memberRepository;
  private final LoginLogRepository loginLogRepository;

  @Value("${ddasoom.service-url}")
  private String serviceUrl;   // 프론트 주소 (기존 프로퍼티 재사용)

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {

      OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
      Long memberId = ((Number) oAuth2User.getAttributes()
              .get(CustomOAuth2UserService.MEMBER_ID_KEY)).longValue();

      // RT 발급 — 일반 로그인과 동일한 부품 조립 (RT에는 role이 없으므로 DB 조회 불필요)
      String refreshToken = jwtUtil.createRefreshToken(memberId);
      redisTokenService.saveRefreshToken(memberId, refreshToken,
              Duration.ofMillis(jwtUtil.getRefreshTokenValidity()));

      ResponseCookie refreshCookie = cookieUtil.createRefreshCookie(
              refreshToken, Duration.ofMillis(jwtUtil.getRefreshTokenValidity()));
      response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

      recordLoginLog(memberId, resolveLoginType(authentication));

      // 프론트 콜백으로 — 이 페이지가 reissue를 호출해 AT를 받는다
      getRedirectStrategy().sendRedirect(request, response, serviceUrl + "/oauth/callback");
  }

  /** registrationId(kakao/naver/google) → LoginType */
  private LoginType resolveLoginType(Authentication authentication) {
      String registrationId = ((OAuth2AuthenticationToken) authentication)
              .getAuthorizedClientRegistrationId();
      return LoginType.valueOf(registrationId.toUpperCase());
  }

  /** 일반 로그인과 동일 원칙 — 로그 실패가 로그인 실패로 이어지지 않도록 격리 */
  private void recordLoginLog(Long memberId, LoginType loginType) {
      try {
          loginLogRepository.save(LoginLog.builder()
                  .member(memberRepository.getReferenceById(memberId))   // 프록시 참조 — 추가 조회 없음
                  .loginType(loginType)
                  .build());
      } catch (Exception e) {
          log.warn("소셜 로그인 로그 기록 실패 (로그인은 정상 처리) - memberId: {}", memberId, e);
      }
  }
}
