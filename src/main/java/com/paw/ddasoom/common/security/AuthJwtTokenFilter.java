package com.paw.ddasoom.common.security;

import java.io.IOException;

import org.springframework.dao.DataAccessException;
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
 *
 * Redis 장애 시에도 fail-close(인증 미설정으로 통과) — 보호 API는 401, 공개 API는 정상.
 * 근거: Redis가 죽으면 로그인/재발급 자체가 불가하므로 fail-open의 실익이 없고,
 *       강제 로그아웃(치안 기능)이 장애를 틈타 무력화되지 않아야 함.
 *
 * 트레이드오프: 인증된 요청마다 Redis 조회가 2회(jti 블랙리스트 + 회원 강제로그아웃) 발생한다.
 *   AT의 완전 무상태 원칙을 일부 양보한 것이지만, "정지된 회원은 즉시 아무것도 못 한다"는
 *   요구를 충족하기 위한 의도적 선택 (SECURITY-FLOW 5절).
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

      // 토큰이 없으면 인증 시도 자체를 건너뛴다 — 공개 API는 그대로 통과, 보호 API는 뒤 인가 단계에서 401.
      // (토큰 없음을 여기서 막지 않는 이유: 공개/보호 구분은 SecurityConfig의 인가 규칙이 담당)
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

  /**
   * 검증 통과 시에만 SecurityContext 등록. 실패 사유는 debug 로그만 (응답은 EntryPoint 담당).
   * 아래 ①~⑤는 순서가 중요하다 — 가벼운 검증(서명·category)을 먼저 하고 Redis 조회(③④)를 뒤에 둬서,
   * 위조/오용 토큰이 불필요하게 Redis를 건드리지 않게 한다.
   */
  private void authenticate(String token) {
      try {
          Claims claims = jwtUtil.parseClaims(token); // ① 서명/만료 검증 (실패 시 JwtException)

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

          // ⑤ 인증 객체 구성 — claims만으로 구성(매 요청 DB 조회 없음).
          // 권한 변경(GUEST→USER)은 여기 반영 안 되고, reissue로 새 AT를 받은 뒤부터 적용된다.
          Role role = jwtUtil.getRole(claims);
          CustomUserDetails userDetails = new CustomUserDetails(memberId, role);

          UsernamePasswordAuthenticationToken authentication =
                  new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(authentication);

      } catch (DataAccessException e) {
          // Redis 조회(블랙리스트/강제로그아웃) 실패 — fail-close: 인증 미설정으로 통과시켜
          // 보호 API는 401로 막고, 강제 로그아웃 차단이 장애를 틈타 뚫리지 않게 한다.
          log.error("인증 중 Redis 접근 실패 — 인증 미설정으로 처리", e);
      } catch (JwtException | IllegalArgumentException e) {
          // 서명 불일치·만료·구조 손상 — 정상적인 "그냥 유효하지 않은 토큰"이라 debug 레벨.
          log.debug("JWT 검증 실패: {}", e.getMessage());
      }
  }

}