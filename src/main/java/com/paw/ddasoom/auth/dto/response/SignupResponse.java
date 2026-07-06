package com.paw.ddasoom.auth.dto.response;

import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {
  private Long memberId;
  private String email;
  private String nickname;
  private Role role;

  // Entity -> DTO 변환을 캡슐화하는 정적 팩토리 메서드
  public static SignupResponse from(Member member) {
      return SignupResponse.builder()
              .memberId(member.getId())
              .email(member.getEmail())
              .nickname(member.getNickname())
              .role(member.getRole())
              .build();
  }
}
