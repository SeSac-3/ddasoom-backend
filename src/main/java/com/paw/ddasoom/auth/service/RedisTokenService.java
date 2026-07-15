package com.paw.ddasoom.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisTokenService {
  private final RedisTemplate<String, String> redisTemplate;

  // 멀티탭 동시 reissue 경합 흡수 창 — "RT 로테이션 + grace 30초"
  private static final Duration GRACE_TTL = Duration.ofSeconds(30);

  // 키 설계 (컨벤션: {용도}:{식별자})
  private String refreshKey(Long memberId) { return "refresh:" + memberId; }
  private String graceKey(Long memberId) { return "graceRefresh:" + memberId; }
  private String blacklistKey(String jti) { return "blacklist:" + jti; }
  // 강제 로그아웃(관리자 강제탈퇴 등) — 대상 회원의 AT를 "무엇인지 몰라도" 즉시 무효화하기 위한 회원 단위 차단.
  // jti 블랙리스트(토큰 단위)로는 "제3자가 그 사람의 현재 AT가 뭔지 모르는" 상황을 못 막아서 별도 설계.
  private String forceLogoutKey(Long memberId) { return "forceLogout:" + memberId; }

  // 비밀번호 재설정 토큰 — 메일 문구 "30분"과 일치
  private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
  private String resetTokenKey(String token) { return "resetToken:" + token; }

  /** 계정 복구 시 강제 로그아웃 마커 해제 — restore가 마커 TTL보다 먼저 오는 경우 필수 (A-1) */
  public void clearForceLogout(Long memberId) {
      redisTemplate.delete(forceLogoutKey(memberId));
  }

  // ── Refresh Token ──
  public void saveRefreshToken(Long memberId, String refreshToken, Duration ttl) {
      redisTemplate.opsForValue().set(refreshKey(memberId), refreshToken, ttl);
  }

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

  /** 로그아웃/강제 로그아웃 — 주 키와 grace 키 동시 삭제 */
  public void deleteRefreshTokens(Long memberId) {
      redisTemplate.delete(refreshKey(memberId));
      redisTemplate.delete(graceKey(memberId));
  }

  /** AT 최대 수명만큼만 걸어두면 충분 — 그 이후엔 자연 만료라 굳이 영구 차단할 필요 없음 */
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
          return; // 이미 만료된 토큰은 등록 불필요
      }
      redisTemplate.opsForValue().set(blacklistKey(jti), "logout", Duration.ofMillis(remainingMillis));
  }

  public boolean isBlacklisted(String jti) {
      return redisTemplate.hasKey(blacklistKey(jti));
  }

  // ── 이메일 인증을 연동한 비밀번호 재설정 요청 ──
  public void saveResetToken(String token, Long memberId) {
        redisTemplate.opsForValue().set(resetTokenKey(token), String.valueOf(memberId), RESET_TOKEN_TTL);
  }

  /** 토큰으로 memberId 조회 — 없으면(만료/위조) null */
  public Long findMemberIdByResetToken(String token) {
    String memberId = redisTemplate.opsForValue().get(resetTokenKey(token));
    return memberId != null ? Long.valueOf(memberId) : null;
  }

  public void deleteResetToken(String token) {
    redisTemplate.delete(resetTokenKey(token));
  }
}
