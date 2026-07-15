package com.paw.ddasoom.common.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.paw.ddasoom.auth.service.RedisTokenService;
import com.paw.ddasoom.auth.util.JwtUtil;
import com.paw.ddasoom.member.domain.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authorization: Bearer {AT} 검증 필터.
 * 검증 실패 시 예외를 던지지 않고 인증 미설정 상태로 체인을 통과시킴
 * → 인가 단계에서 미인증 판정 → CustomAuthenticationEntryPoint가 401 응답
 * (필터에서 throw하면 GlobalExceptionHandler가 못 잡고 500이 떨어짐)
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthJwtTokenFilter extends OncePerRequestFilter{

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtUtil jwtUtil;
  private final RedisTokenService redisTokenService;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

      String token = resolveToken(request);

      if (token != null) {
          authenticate(token);
      }
      filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
      String header = request.getHeader(AUTHORIZATION_HEADER);
      if (header == null || !header.startsWith(BEARER_PREFIX)) {
          return null;
      }
      return header.substring(BEARER_PREFIX.length());
  }

  /** 검증 통과 시에만 SecurityContext 등록. 실패 사유는 debug 로그만 (응답은 EntryPoint 담당) */
  private void authenticate(String token) {
      try {
          Claims claims = jwtUtil.parseClaims(token); // ① 서명/만료 검증

          // ② RT를 AT 자리에 꽂는 오용 차단 — category claim 확인
          if (!JwtUtil.CATEGORY_ACCESS.equals(jwtUtil.getCategory(claims))) {
              log.debug("AT가 아닌 토큰으로 인증 시도 차단");
              return;
          }

          // ③ 로그아웃된 AT 차단 (토큰 단위 — jti 블랙리스트)
          if (redisTokenService.isBlacklisted(jwtUtil.getJti(claims))) {
              log.debug("블랙리스트 등록된 AT 차단");
              return;
          }

          Long memberId = jwtUtil.getMemberId(claims);

          // ④ 강제 로그아웃 차단 (회원 단위 — 탈퇴/강제탈퇴 회원의 기발급 AT 전부)
          if (redisTokenService.isForceLogout(memberId)) {
              log.debug("강제 로그아웃 대상 회원의 AT 차단 - memberId: {}", memberId);
              return;
          }

          Role role = jwtUtil.getRole(claims); // ⑤ 인증 객체 구성
          CustomUserDetails userDetails = new CustomUserDetails(memberId, role);

          UsernamePasswordAuthenticationToken authentication =
                  new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(authentication);

      } catch (JwtException | IllegalArgumentException e) {
          log.debug("JWT 검증 실패: {}", e.getMessage());
      }
  }

}
