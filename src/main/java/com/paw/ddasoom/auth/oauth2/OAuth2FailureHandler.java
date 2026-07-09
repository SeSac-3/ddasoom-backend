package com.paw.ddasoom.auth.oauth2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.paw.ddasoom.auth.exception.AuthErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 소셜 로그인 실패 처리 — 에러코드를 프론트 콜백의 쿼리로 전달.
 * (없으면 Spring 기본 /login?error HTML로 떨어짐 — EntryPoint와 같은 원리의 필수 세트)
 * 에러코드는 토큰이 아니므로 URL 쿼리에 실려도 무해하다.
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler{

  @Value("${ddasoom.service-url}")
  private String serviceUrl;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException {

      String errorCode = resolveErrorCode(exception);
      log.debug("소셜 로그인 실패 - code: {}", errorCode);

      String targetUrl = UriComponentsBuilder
              .fromUriString(serviceUrl + "/oauth/callback")
              .queryParam("error", errorCode)
              .build().toUriString();

      getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }

  /**
   * 우리가 던진 코드(AUTH_106/107)는 그대로 전달.
   * provider발 에러(사용자가 동의 화면에서 취소 → access_denied 등)는 AUTH_102로 일반화
   * — 프론트가 provider별 에러 문자열까지 분기할 필요 없도록.
   */
  private String resolveErrorCode(AuthenticationException exception) {
      if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
          String code = oAuth2Exception.getError().getErrorCode();
          if (code != null && code.startsWith("AUTH_")) {
              return code;
          }
      }
      return AuthErrorCode.UNAUTHORIZED.getCode();
  }

}
