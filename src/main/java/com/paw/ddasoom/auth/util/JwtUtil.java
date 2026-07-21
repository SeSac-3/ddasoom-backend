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

/**
 * JWT 생성·파싱 전담 유틸.
 *
 * <p>이 클래스는 "토큰을 만들고 읽는" 순수 기능만 담당한다. 토큰의 유효성 판단 중
 * 서명·만료는 여기서(parseClaims) 처리하지만, <b>블랙리스트·강제로그아웃 같은 상태 기반 무효화는
 * 여기 책임이 아니다</b> — 그건 Redis를 아는 필터(AuthJwtTokenFilter)와 RedisTokenService의 몫이다.
 * (JWT의 무상태 원칙과 우리 서비스의 상태 기반 차단을 계층으로 분리한 설계)
 *
 * <p>AT/RT claims 구성은 SECURITY-FLOW 문서 5절과 일치:
 * AT = sub(memberId) + role + category + jti / RT = sub + category + jti (role 미포함).
 */
@Component
public class JwtUtil {

  // category claim 값 — AT/RT를 같은 서명키로 발급하므로, "이 토큰이 AT인가 RT인가"를
  // category로 구분한다. 이게 없으면 RT를 AT 자리에 넣는 토큰 혼용 공격을 막을 수 없다.
  public static final String CATEGORY_ACCESS = "access";
  public static final String CATEGORY_REFRESH = "refresh";

  private final SecretKey secretKey;
  private final long accessTokenValidity;   // ms
  private final long refreshTokenValidity;  // ms

  // 시크릿·TTL은 yml(환경변수)에서 주입 — 하드코딩 금지(A-0 환경변수 전환). secret은 BASE64 디코딩 후 HMAC 키로.
  public JwtUtil(@Value("${ddasoom.jwt.secret}") String secret,
                  @Value("${ddasoom.jwt.access-token-validity}") long accessTokenValidity,
                  @Value("${ddasoom.jwt.refresh-token-validity}") long refreshTokenValidity) {
      this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
      this.accessTokenValidity = accessTokenValidity;
      this.refreshTokenValidity = refreshTokenValidity;
  }

  /** AT 생성 — sub=memberId, role, category=access, jti. 권한 판단에 쓰이므로 role 포함. */
  public String createAccessToken(Long memberId, Role role) {
      return createToken(memberId, CATEGORY_ACCESS, accessTokenValidity, role);
  }

  /**
   * RT 생성 — role 미포함.
   * RT는 오직 "재발급 자격 증명"으로만 쓰이고 권한 판단에 관여하지 않는다. role을 넣으면
   * 권한 변경(GUEST→USER) 후에도 낡은 role이 RT에 남아 혼란을 부르므로 의도적으로 제외.
   */
  public String createRefreshToken(Long memberId) {
      return createToken(memberId, CATEGORY_REFRESH, refreshTokenValidity, null);
  }

  // 블랙리스트/강제로그아웃 마커의 TTL 계산 등에서 AT 최대 수명이 필요할 때 재사용.
  // (동일 값을 여러 곳에서 상수로 중복 선언하지 않도록 이 필드를 단일 출처로 노출)
  public long getAccessTokenValidityMillis() {
      return accessTokenValidity;
  }

  private String createToken(Long memberId, String category, long validity, Role role) {
      Date now = new Date();
      var builder = Jwts.builder()
              .subject(String.valueOf(memberId))         // sub = 회원 PK. 토큰에서 신원은 항상 여기서만 꺼낸다
              .id(UUID.randomUUID().toString())          // jti — 로그아웃 블랙리스트 키(토큰 단위 차단의 식별자)
              .claim("category", category)
              .issuedAt(now)
              .expiration(new Date(now.getTime() + validity))
              .signWith(secretKey);
      // role은 AT에만 존재 → RT 생성 시 null이 넘어오므로 조건부로만 추가
      if (role != null) {
          builder.claim("role", role.name());
      }
      return builder.compact();
  }

  /**
   * 파싱 + 서명·만료 검증.
   *
   * <p>서명 불일치·만료·구조 손상이면 JwtException 계열 예외가 던져진다(만료는 ExpiredJwtException).
   * 이 클래스는 예외를 삼키지 않고 그대로 전파한다 — "왜 실패했는지"의 판단과 응답 변환은
   * 호출자(AuthJwtTokenFilter)의 책임이기 때문. 여기서 잡아 null을 반환하면 실패 원인이 사라진다.
   */
  public Claims parseClaims(String token) {
      return Jwts.parser()
              .verifyWith(secretKey)
              .build()
              .parseSignedClaims(token)
              .getPayload();
  }

  // ── claims 접근자 — 토큰의 각 필드를 타입 안전하게 꺼내는 얇은 래퍼 ──
  // 컨트롤러/서비스가 claims 키 문자열("role" 등)을 직접 다루지 않도록 여기로 모은다.
  public Long getMemberId(Claims claims) { return Long.valueOf(claims.getSubject()); }
  public String getCategory(Claims claims) { return claims.get("category", String.class); }
  public Role getRole(Claims claims) { return Role.valueOf(claims.get("role", String.class)); }
  public String getJti(Claims claims) { return claims.getId(); }

  /**
   * 토큰의 남은 유효시간(ms). 로그아웃 시 블랙리스트 키의 TTL로 사용한다.
   * 남은 시간만큼만 Redis에 담아두면, AT가 어차피 만료될 시점에 블랙리스트 항목도 자동 소멸 → 메모리 누수 방지.
   */
  public long getRemainingMillis(Claims claims) {
      return claims.getExpiration().getTime() - System.currentTimeMillis();
  }

  public long getAccessTokenValidity() { return accessTokenValidity; }
  public long getRefreshTokenValidity() { return refreshTokenValidity; }
}
