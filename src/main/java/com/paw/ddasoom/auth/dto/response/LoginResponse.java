package com.paw.ddasoom.auth.dto.response;

import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;

import lombok.Builder;
import lombok.Getter;

/** 로그인/재발급 응답 — 비밀번호 해시 등 민감정보 절대 미포함 
 * Reissue 도 해당 DTO 재사용
 */
@Getter
@Builder
public class LoginResponse {
  private String accessToken;
  private long expiresIn;        // AT 유효시간(초) — 프론트 상태 변수 관리용
  private Long memberId;
  private String email;
  private String nickname;
  private Role role;

  public static LoginResponse of(String accessToken, long expiresInSeconds, Member member) {
      return LoginResponse.builder()
              .accessToken(accessToken)
              .expiresIn(expiresInSeconds)
              .memberId(member.getId())
              .email(member.getEmail())
              .nickname(member.getNickname())
              .role(member.getRole())
              .build();
  }
}
