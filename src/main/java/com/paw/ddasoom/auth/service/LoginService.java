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
import com.paw.ddasoom.member.domain.MemberStatus;
import com.paw.ddasoom.member.repository.MemberRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로그인·토큰 재발급·로그아웃 — 세션(토큰) 수명주기를 다루는 서비스.
 *
 * <p>회원가입·이메일 인증은 AuthService, 토큰 자체의 생성/파싱은 JwtUtil, Redis 키 조작은
 * RedisTokenService가 맡고, 이 클래스는 그것들을 엮어 "로그인 상태를 만들고·갱신하고·끝내는" 흐름을 조율한다.
 *
 * <p>보안 설계의 핵심 두 축:
 * ① 실패 응답 단일화(AUTH_101) — 계정 유무를 노출하지 않아 열거 공격을 막는다.
 * ② RT 로테이션 + grace — 탈취된 RT 수명을 최소화하면서 멀티탭 동시 재발급 경합은 흡수한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

  private final MemberRepository memberRepository;
  private final LoginLogRepository loginLogRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final RedisTokenService redisTokenService;

  /** 로그인 결과 — 컨트롤러가 RT로 쿠키를 조립하고, response만 body로 내림 (RT를 body에 노출하지 않기 위한 분리) */
  public record LoginResult(LoginResponse response, String refreshToken) {}

    /**
   * BCrypt 비교 — 입력이 72바이트를 초과하면 버전에 따라 IllegalArgumentException을 던질 수 있음.
   * LoginRequest.password는 형식 검증이 없어(@Pattern 미적용, 길이만 @Size(max=64) 제한) 멀티바이트
   * 문자(한글 등)로 64자를 채우면 바이트 수가 72를 넘을 수 있다 — 이 경우도 "불일치"로 수렴시켜
   * 로그인 실패 단일화(AUTH_101) 정책을 유지한다. (예외가 새어나가면 500이 떨어져 "특이 입력"이 노출됨)
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

      // ── C안: 비밀번호를 '먼저' 검증한다 ──────────────────────────────────
      // 계정 없음 / 소셜 전용(비번 null) / 비번 불일치는 전부 AUTH_101 하나로 — 열거 공격 방지.
      // 이 관문을 통과하지 못하면 아래 상태(탈퇴/숨김) 사유는 절대 노출되지 않는다.
      // → "정당한 본인(비번을 아는 사람)"에게만 탈퇴/숨김 사유를 안내하는 것이 C안의 핵심.
      boolean isPasswordInvalid = member.getPassword() == null   // 소셜 전용 회원 — 비밀번호 로그인 불가
              || !matchesPassword(request.getPassword(), member.getPassword());
      if (isPasswordInvalid) {
          throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
      }

      // ── 비번이 맞은 뒤에만 계정 상태를 검사해 '구분된' 사유를 안내한다 ──────
      // 탈퇴가 숨김보다 우선(탈퇴는 계정 자체가 소멸된 더 상위 상태).
      if (member.isDeleted()) {
          throw new AuthException(AuthErrorCode.WITHDRAWN_MEMBER);   // AUTH_109
      }
      if (member.getStatus() == MemberStatus.HIDDEN) {
          throw new AuthException(AuthErrorCode.BLOCKED_MEMBER);     // AUTH_110
      }

      String accessToken = jwtUtil.createAccessToken(member.getId(), member.getRole());
      String refreshToken = jwtUtil.createRefreshToken(member.getId());
      redisTokenService.saveRefreshToken(member.getId(), refreshToken,
              Duration.ofMillis(jwtUtil.getRefreshTokenValidity()));

      recordLoginLog(member, LoginType.LOCAL);

      // expiresIn은 초 단위로 내린다(프론트 타이머용) — JwtUtil은 ms라 /1000
      return new LoginResult(
              LoginResponse.of(accessToken, jwtUtil.getAccessTokenValidity() / 1000, member),
              refreshToken);
  }

  /**
   * 토큰 재발급 — RT 로테이션 + grace 30초 (SECURITY-FLOW.md 2번 흐름 그대로)
   * 반환의 refreshToken이 null이면 grace 통과 → 컨트롤러는 쿠키를 재설정하지 않음
   *
   * readOnly 트랜잭션: 이 메서드의 DB 접근은 회원 조회뿐(RT 저장은 Redis라 JPA 트랜잭션 대상 아님).
   */
  @Transactional(readOnly = true)
  public LoginResult reissue(String refreshToken) {
      Claims claims = parseRefreshClaims(refreshToken);
      Long memberId = jwtUtil.getMemberId(claims);

      Member member = memberRepository.findById(memberId)
              .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

      // 탈퇴 회원 차단 — RT 삭제(부수효과)에 간접 의존하지 않는 심층 방어.
      // withdraw가 RT를 지우므로 정상 경로에선 아래 대조에서 이미 걸러지지만,
      // 여기서 명시적으로 막아 "탈퇴 회원에게 새 AT 발급"을 원천 차단한다.
      if (member.isDeleted()) {
          throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
      }

      // 주 키 일치 → 정상 회전: 새 AT+새 RT 발급, 구 RT는 grace 창(30초)에 넣어 뒤늦은 동시 요청을 흡수
      if (redisTokenService.matchesRefreshToken(memberId, refreshToken)) {
          String newAccessToken = jwtUtil.createAccessToken(memberId, member.getRole());
          String newRefreshToken = jwtUtil.createRefreshToken(memberId);
          redisTokenService.saveGraceToken(memberId, refreshToken);
          redisTokenService.saveRefreshToken(memberId, newRefreshToken,
                  Duration.ofMillis(jwtUtil.getRefreshTokenValidity()));

          return new LoginResult(
                  LoginResponse.of(newAccessToken, jwtUtil.getAccessTokenValidity() / 1000, member),
                  newRefreshToken);
      }

      // grace 통과(직전 회전된 구 RT — 멀티탭 동시 요청) → 새 AT만 발급하고 재회전은 금지.
      // 재회전을 허용하면 회전 체인이 무한히 이어져 grace의 "경합 흡수" 목적이 깨진다.
      if (redisTokenService.matchesGraceToken(memberId, refreshToken)) {
          String newAccessToken = jwtUtil.createAccessToken(memberId, member.getRole());
          return new LoginResult(
                  LoginResponse.of(newAccessToken, jwtUtil.getAccessTokenValidity() / 1000, member),
                  null);
      }

      // 주 키·grace 둘 다 불일치 → 이미 회전이 두 번 이상 지난 낡은 RT거나 위조 → 재로그인 유도
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
  }

  /** 로그아웃 — Redis 세션 삭제 + AT jti 블랙리스트. accessToken은 컨트롤러가 헤더에서 추출해 전달 */
  public void logout(Long memberId, String accessToken) {
      redisTokenService.deleteRefreshTokens(memberId);

      try {
          Claims claims = jwtUtil.parseClaims(accessToken);
          redisTokenService.addBlacklist(jwtUtil.getJti(claims), jwtUtil.getRemainingMillis(claims));
      } catch (JwtException | IllegalArgumentException e) {
          // AT 파싱 실패여도 세션은 이미 삭제됨 — 로그아웃은 성공 처리 (재발급 경로가 끊긴 것으로 충분)
          log.debug("로그아웃 시 AT 파싱 실패 (블랙리스트 생략): {}", e.getMessage());
      }
  }

  // RT 전용 파싱 — 서명·만료 검증에 더해 category=refresh까지 확인.
  // 모든 실패(위조·만료·AT 오용)를 INVALID_REFRESH_TOKEN 하나로 수렴시켜 프론트가 "재로그인"으로 단순 대응하게 한다.
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
