package com.paw.ddasoom.member.dto.response;

import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {
  private Long memberId;
  private String email;
  private String name;
  private String nickname;
  private String tel;
  private Role role;

  public static MemberResponse from(Member member) {
      return MemberResponse.builder()
              .memberId(member.getId())
              .email(member.getEmail())
              .name(member.getName())
              .nickname(member.getNickname())
              .tel(member.getTel())
              .role(member.getRole())
              .build();
  }
}
