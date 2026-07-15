package com.paw.ddasoom.auth.service;

import java.time.Duration;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.auth.domain.LoginLog;
import com.paw.ddasoom.auth.domain.LoginType;
import com.paw.ddasoom.auth.dto.request.LoginRequest;
import com.paw.ddasoom.auth.dto.response.LoginResponse;
import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.auth.exception.AuthException;
import com.paw.ddasoom.auth.repository.LoginLogRepository;
import com.paw.ddasoom.auth.util.JwtUtil;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.repository.MemberRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

  private final MemberRepository memberRepository;
  private final LoginLogRepository loginLogRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final RedisTokenService redisTokenService;

  /** 로그인 결과 — 컨트롤러가 RT로 쿠키를 조립하고, response만 body로 내림 */
  public record LoginResult(LoginResponse response, String refreshToken) {}

    /**
   * BCrypt 비교 — 입력이 72바이트를 초과하면 버전에 따라 IllegalArgumentException을 던질 수 있음.
   * LoginRequest.password는 형식 검증이 없어(@Pattern 미적용, 길이만 @Size(max=64) 제한) 멀티바이트
   * 문자(한글 등)로 64자를 채우면 바이트 수가 72를 넘을 수 있다 — 이 경우도 "불일치"로 수렴시켜
   * 로그인 실패 단일화(AUTH_101) 정책을 유지한다.
   */
  private boolean matchesPassword(String rawPassword, String encodedPassword) {
      try {
          return passwordEncoder.matches(rawPassword, encodedPassword);
      } catch (IllegalArgumentException e) {
          return false;
      }
  }

  /**
   * 일반 로그인.
   * 계정 없음/비밀번호 불일치/탈퇴 회원을 전부 INVALID_CREDENTIALS(401) 하나로 응답
   * — 구분 노출 시 이메일 존재 여부 열거 공격이 가능 (컨벤션 5. 예외 사용 규칙)
   *
   * @Transactional 미사용: DB 쓰기는 LoginLog save 하나뿐(자체 원자적)이며,
   * 트랜잭션으로 묶으면 로그 실패 시 rollback-only 마킹으로 로그인까지 실패할 수 있음
   */
  public LoginResult login(LoginRequest request) {
      Member member = memberRepository.findByEmail(request.getEmail())
              .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

      boolean isLoginBlocked = member.isDeleted()
              || member.getPassword() == null   // 소셜 전용 회원 — 비밀번호 로그인 불가
              || !matchesPassword(request.getPassword(), member.getPassword());
      if (isLoginBlocked) {
          throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
      }

      String accessToken = jwtUtil.createAccessToken(member.getId(), member.getRole());
      String refreshToken = jwtUtil.createRefreshToken(member.getId());
      redisTokenService.saveRefreshToken(member.getId(), refreshToken,
              Duration.ofMillis(jwtUtil.getRefreshTokenValidity()));

      recordLoginLog(member, LoginType.LOCAL);

      return new LoginResult(
              LoginResponse.of(accessToken, jwtUtil.getAccessTokenValidity() / 1000, member),
              refreshToken);
  }

  /**
   * 토큰 재발급 — RT 로테이션 + grace 30초 (SECURITY-FLOW.md 2번 흐름 그대로)
   * 반환의 refreshToken이 null이면 grace 통과 → 컨트롤러는 쿠키를 재설정하지 않음
   */
  @Transactional(readOnly = true)
  public LoginResult reissue(String refreshToken) {
      Claims claims = parseRefreshClaims(refreshToken);
      Long memberId = jwtUtil.getMemberId(claims);

      Member member = memberRepository.findById(memberId)
              .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

      if (redisTokenService.matchesRefreshToken(memberId, refreshToken)) {
          // 주 키 일치 → 회전: 새 RT 발급, 구 RT는 grace 창(30초)으로
          String newAccessToken = jwtUtil.createAccessToken(memberId, member.getRole());
          String newRefreshToken = jwtUtil.createRefreshToken(memberId);
          redisTokenService.saveGraceToken(memberId, refreshToken);
          redisTokenService.saveRefreshToken(memberId, newRefreshToken,
                  Duration.ofMillis(jwtUtil.getRefreshTokenValidity()));

          return new LoginResult(
                  LoginResponse.of(newAccessToken, jwtUtil.getAccessTokenValidity() / 1000, member),
                  newRefreshToken);
      }

      if (redisTokenService.matchesGraceToken(memberId, refreshToken)) {
          // grace 통과(직전 회전된 구 RT — 멀티탭 동시 요청) → 새 AT만, 재회전 금지
          String newAccessToken = jwtUtil.createAccessToken(memberId, member.getRole());
          return new LoginResult(
                  LoginResponse.of(newAccessToken, jwtUtil.getAccessTokenValidity() / 1000, member),
                  null);
      }

      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
  }

  /** 로그아웃 — Redis 세션 삭제 + AT jti 블랙리스트. accessToken은 컨트롤러가 헤더에서 추출해 전달 */
  public void logout(Long memberId, String accessToken) {
      redisTokenService.deleteRefreshTokens(memberId);

      try {
          Claims claims = jwtUtil.parseClaims(accessToken);
          redisTokenService.addBlacklist(jwtUtil.getJti(claims), jwtUtil.getRemainingMillis(claims));
      } catch (JwtException | IllegalArgumentException e) {
          // AT 파싱 실패여도 세션은 이미 삭제됨 — 로그아웃은 성공 처리
          log.debug("로그아웃 시 AT 파싱 실패 (블랙리스트 생략): {}", e.getMessage());
      }
  }

  private Claims parseRefreshClaims(String refreshToken) {
      try {
          Claims claims = jwtUtil.parseClaims(refreshToken);
          // AT를 RT 자리에 꽂는 오용 차단
          if (!JwtUtil.CATEGORY_REFRESH.equals(jwtUtil.getCategory(claims))) {
              throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
          }
          return claims;
      } catch (JwtException | IllegalArgumentException e) {
          throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
      }
  }

  /** 로그인 로그 실패가 로그인 실패로 이어지면 안 됨 — 격리 (컨벤션 6. 외부 I/O 격리와 동일 원칙) */
  private void recordLoginLog(Member member, LoginType loginType) {
      try {
          loginLogRepository.save(LoginLog.builder()
                  .member(member)
                  .loginType(loginType)
                  .build());
      } catch (Exception e) {
          log.warn("로그인 로그 기록 실패 (로그인은 정상 처리) - memberId: {}", member.getId(), e);
      }
  }
}
