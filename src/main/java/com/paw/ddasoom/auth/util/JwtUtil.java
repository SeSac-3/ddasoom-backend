package com.paw.ddasoom.auth.util;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.paw.ddasoom.member.domain.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
  public static final String CATEGORY_ACCESS = "access";
  public static final String CATEGORY_REFRESH = "refresh";

  private final SecretKey secretKey;
  private final long accessTokenValidity;   // ms
  private final long refreshTokenValidity;  // ms

  public JwtUtil(@Value("${ddasoom.jwt.secret}") String secret,
                  @Value("${ddasoom.jwt.access-token-validity}") long accessTokenValidity,
                  @Value("${ddasoom.jwt.refresh-token-validity}") long refreshTokenValidity) {
      this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
      this.accessTokenValidity = accessTokenValidity;
      this.refreshTokenValidity = refreshTokenValidity;
  }

  /** AT 생성 — sub=memberId, role, category, jti (SECURITY-FLOW.md 5번 claims 구성) */
  public String createAccessToken(Long memberId, Role role) {
      return createToken(memberId, CATEGORY_ACCESS, accessTokenValidity, role);
  }

  /** RT 생성 — role 미포함 */
  public String createRefreshToken(Long memberId) {
      return createToken(memberId, CATEGORY_REFRESH, refreshTokenValidity, null);
  }

  // 클래스 내 이미 AT TTL을 정의한 필드가 있다면 그 필드를 재사용 — 새 상수를 중복 선언하지 않는다
  public long getAccessTokenValidityMillis() {
      return accessTokenValidity; // 필드명은 실제 클래스의 AT TTL 필드명에 맞춰 조정
  }

  private String createToken(Long memberId, String category, long validity, Role role) {
      Date now = new Date();
      var builder = Jwts.builder()
              .subject(String.valueOf(memberId))
              .id(UUID.randomUUID().toString())          // jti — 블랙리스트 키
              .claim("category", category)
              .issuedAt(now)
              .expiration(new Date(now.getTime() + validity))
              .signWith(secretKey);
      if (role != null) {
          builder.claim("role", role.name());
      }
      return builder.compact();
  }

  /**
   * 파싱 + 서명/만료 검증.
   * 실패 시 JwtException(만료: ExpiredJwtException) 발생 — 호출자가 처리
   */
  public Claims parseClaims(String token) {
      return Jwts.parser()
              .verifyWith(secretKey)
              .build()
              .parseSignedClaims(token)
              .getPayload();
  }

  public Long getMemberId(Claims claims) { return Long.valueOf(claims.getSubject()); }
  public String getCategory(Claims claims) { return claims.get("category", String.class); }
  public Role getRole(Claims claims) { return Role.valueOf(claims.get("role", String.class)); }
  public String getJti(Claims claims) { return claims.getId(); }

  /** 블랙리스트 TTL용 — 토큰의 남은 유효시간(ms) */
  public long getRemainingMillis(Claims claims) {
      return claims.getExpiration().getTime() - System.currentTimeMillis();
  }

  public long getAccessTokenValidity() { return accessTokenValidity; }
  public long getRefreshTokenValidity() { return refreshTokenValidity; }


}
