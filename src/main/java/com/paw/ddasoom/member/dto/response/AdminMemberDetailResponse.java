package com.paw.ddasoom.member.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.paw.ddasoom.auth.domain.LoginLog;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.MemberSocial;
import com.paw.ddasoom.member.domain.MemberStatus;
import com.paw.ddasoom.member.domain.Role;

import lombok.Builder;
import lombok.Getter;

/** 관리자 회원 상세 — 기본정보 + 소셜 연동 + 최근 로그인 이력 5건 */
@Getter
@Builder
public class AdminMemberDetailResponse {
  private Long memberId;
  private String email;
  private String name;
  private String nickname;
  private String tel;
  private Role role;
  private MemberStatus status;   // ACTIVE=정상 / HIDDEN=신고 제재 숨김 (탈퇴 여부와 별개 축)
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;
  private List<String> socialProviders;            // 예: ["KAKAO", "GOOGLE"]
  private List<LoginLogResponse> recentLoginLogs;  // 최근 5건 미리보기 (전체는 /login-logs)

  public static AdminMemberDetailResponse of(Member member,
                                              List<MemberSocial> socials,
                                              List<LoginLog> recentLogs) {
      return AdminMemberDetailResponse.builder()
              .memberId(member.getId())
              .email(member.getEmail())
              .name(member.getName())
              .nickname(member.getNickname())
              .tel(member.getTel())
              .status(member.getStatus())
              .role(member.getRole())
              .createdAt(member.getCreatedAt())
              .updatedAt(member.getUpdatedAt())
              .deletedAt(member.getDeletedAt())
              .socialProviders(socials.stream()
                      .map(social -> social.getProvider().name())
                      .toList())
              .recentLoginLogs(recentLogs.stream()
                      .map(LoginLogResponse::from)
                      .toList())
              .build();
  }
}
