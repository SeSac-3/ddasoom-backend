package com.paw.ddasoom.member.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;

import lombok.Builder;
import lombok.Getter;

/** 관리자 회원 목록 행 — 상태 구분은 deletedAt(null=활성)으로 프론트가 표시 */
@Getter
@Builder
public class AdminMemberResponse {
  private Long memberId;
  private String email;
  private String nickname;
  private Role role;
  private LocalDateTime createdAt;
  private LocalDateTime deletedAt;

  public static AdminMemberResponse from(Member member) {
      return AdminMemberResponse.builder()
              .memberId(member.getId())
              .email(member.getEmail())
              .nickname(member.getNickname())
              .role(member.getRole())
              .createdAt(member.getCreatedAt())
              .deletedAt(member.getDeletedAt())
              .build();
  }
}
