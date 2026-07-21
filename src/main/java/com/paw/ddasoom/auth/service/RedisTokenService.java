package com.paw.ddasoom.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * 인증 관련 Redis 키를 다루는 단일 창구.
 *
 * <p>토큰의 상태 기반 무효화(로그아웃 블랙리스트·강제로그아웃·RT 대조·grace)와 비밀번호 재설정 토큰을
 * 여기 한 곳에서만 조작한다. JwtUtil이 "토큰을 만들고 읽는" 무상태 계층이라면, 이 클래스는
 * "그 토큰이 지금도 유효한가"라는 상태를 관리하는 계층이다. 키 문자열을 여기 가둬서
 * 서비스·필터가 Redis 키 규칙을 직접 알 필요가 없게 한다.
 *
 * <p>키 설계는 SECURITY-FLOW 7절 Redis 키 표와 일치한다.
 */
@Service
@RequiredArgsConstructor
public class RedisTokenService {
  private final RedisTemplate<String, String> redisTemplate;

  // 멀티탭 동시 reissue 경합 흡수 창 — "RT 로테이션 + grace 30초".
  // 브라우저 재시작 등으로 여러 탭이 동시에 구 RT로 재발급을 요청할 때, 첫 요청이 회전시킨 뒤
  // 구 RT를 이 시간만큼 살려둬 나머지 탭의 요청을 흡수한다(전부 401나는 것 방지).
  private static final Duration GRACE_TTL = Duration.ofSeconds(30);

  // 키 설계 (컨벤션: {용도}:{식별자}) — 키 생성은 private 메서드로 모아 오타·불일치를 원천 차단
  private String refreshKey(Long memberId) { return "refresh:" + memberId; }
  private String graceKey(Long memberId) { return "graceRefresh:" + memberId; }
  private String blacklistKey(String jti) { return "blacklist:" + jti; }
  // 강제 로그아웃(관리자 강제탈퇴 등) — 대상 회원의 AT를 "무엇인지 몰라도" 즉시 무효화하기 위한 회원 단위 차단.
  // jti 블랙리스트(토큰 단위)로는 "제3자가 그 사람의 현재 AT가 뭔지 모르는" 상황을 못 막아서 별도 설계.
  private String forceLogoutKey(Long memberId) { return "forceLogout:" + memberId; }

  // 비밀번호 재설정 토큰 — 메일 문구 "30분"과 일치 (매직넘버 방지, 이유 주석)
  private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
  private String resetTokenKey(String token) { return "resetToken:" + token; }

  /**
   * 계정 복구 시 강제 로그아웃 마커 해제.
   * 복구(restore)는 soft delete 해제(DB)와 이 마커 삭제(Redis)를 함께 되돌리는 하나의 역연산이어야 한다.
   * 마커 TTL(AT 최대 수명)이 지나기 전에 복구가 오면, 이걸 지우지 않는 한 복구된 회원이 계속 차단되므로 필수.
   */
  public void clearForceLogout(Long memberId) {
      redisTemplate.delete(forceLogoutKey(memberId));
  }

  // ── Refresh Token — 주 키. 로그인 시 저장, 재발급 시 대조·교체(로테이션) ──
  public void saveRefreshToken(Long memberId, String refreshToken, Duration ttl) {
      redisTemplate.opsForValue().set(refreshKey(memberId), refreshToken, ttl);
  }

  // 저장된 RT와 완전 일치해야 통과. 단일 세션 정책상 새 기기 로그인 시 이 값이 교체되어
  // 기존 기기의 RT는 자동으로 불일치 → 재발급 실패한다(별도 로그아웃 처리 없이 세션 만료).
  public boolean matchesRefreshToken(Long memberId, String refreshToken) {
      String stored = redisTemplate.opsForValue().get(refreshKey(memberId));
      return stored != null && stored.equals(refreshToken);
  }

  // ── Grace (회전 직후 구 RT 30초 유예) ──
  public void saveGraceToken(Long memberId, String oldRefreshToken) {
      redisTemplate.opsForValue().set(graceKey(memberId), oldRefreshToken, GRACE_TTL);
  }

  public boolean matchesGraceToken(Long memberId, String refreshToken) {
      String stored = redisTemplate.opsForValue().get(graceKey(memberId));
      return stored != null && stored.equals(refreshToken);
  }

  /** 로그아웃/강제 로그아웃/탈퇴 — 주 키와 grace 키를 함께 삭제해 재발급 경로를 완전 차단 */
  public void deleteRefreshTokens(Long memberId) {
      redisTemplate.delete(refreshKey(memberId));
      redisTemplate.delete(graceKey(memberId));
  }

  /**
   * 강제 로그아웃 마커 등록.
   * TTL을 AT 최대 수명만큼만 걸어두면 충분하다 — 그 시간이 지나면 대상 AT는 어차피 자연 만료되므로
   * 영구 차단할 필요가 없다. ttlMillis<=0(이미 만료)이면 등록 자체를 건너뛴다.
   */
  public void markForceLogout(Long memberId, long ttlMillis) {
      if (ttlMillis <= 0) {
          return;
      }
      redisTemplate.opsForValue().set(forceLogoutKey(memberId), "forced", Duration.ofMillis(ttlMillis));
  }

  public boolean isForceLogout(Long memberId) {
      return redisTemplate.hasKey(forceLogoutKey(memberId));
  }

  // ── Blacklist (로그아웃된 AT의 jti, 남은 유효시간만큼) ──
  public void addBlacklist(String jti, long remainingMillis) {
      if (remainingMillis <= 0) {
          return; // 이미 만료된 토큰은 등록 불필요 — 넣어봐야 즉시 만료라 메모리만 낭비
      }
      redisTemplate.opsForValue().set(blacklistKey(jti), "logout", Duration.ofMillis(remainingMillis));
  }

  public boolean isBlacklisted(String jti) {
      return redisTemplate.hasKey(blacklistKey(jti));
  }

  // ── 비밀번호 재설정 토큰 — 일회용. 발급 시 저장, 사용 시 조회 후 삭제 ──
  public void saveResetToken(String token, Long memberId) {
        redisTemplate.opsForValue().set(resetTokenKey(token), String.valueOf(memberId), RESET_TOKEN_TTL);
  }

  /** 토큰으로 memberId 조회 — 없으면(만료/위조) null. 호출자는 null을 "무효 토큰"으로 처리 */
  public Long findMemberIdByResetToken(String token) {
    String memberId = redisTemplate.opsForValue().get(resetTokenKey(token));
    return memberId != null ? Long.valueOf(memberId) : null;
  }

  public void deleteResetToken(String token) {
    redisTemplate.delete(resetTokenKey(token));
  }
}
