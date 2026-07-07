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
}
