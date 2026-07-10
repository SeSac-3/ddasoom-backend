package com.paw.ddasoom.member.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.auth.domain.LoginLog;
import com.paw.ddasoom.auth.repository.LoginLogRepository;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;
import com.paw.ddasoom.member.dto.response.AdminMemberDetailResponse;
import com.paw.ddasoom.member.dto.response.AdminMemberResponse;
import com.paw.ddasoom.member.dto.response.LoginLogResponse;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.member.repository.MemberSocialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

  private final MemberRepository memberRepository;
  private final MemberSocialRepository memberSocialRepository;
  private final LoginLogRepository loginLogRepository;
  private final MemberService memberService;   // getMember, withdraw 재사용

  /** 목록 — 탈퇴 회원 포함 전체. 상태 구분은 응답의 deletedAt으로 프론트가 표시 */
  @Transactional(readOnly = true)
  public PageResponse<AdminMemberResponse> getMembers(String keyword, Role role, Pageable pageable) {
      // 빈 문자열 keyword는 null로 정규화 (JPQL 조건 미적용)
      String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;

      Page<Member> page = memberRepository.searchForAdmin(normalizedKeyword, role, pageable);
      return PageResponse.of(page, AdminMemberResponse::from);
  }

  /** 상세 — 탈퇴 회원도 조회 가능 (강제탈퇴 소명·복구 판단용) */
  @Transactional(readOnly = true)
  public AdminMemberDetailResponse getMemberDetail(Long memberId) {
      Member member = memberService.getMember(memberId);
      return AdminMemberDetailResponse.of(
              member,
              memberSocialRepository.findByMemberId(memberId),
              loginLogRepository.findTop5ByMemberIdOrderByCreatedAtDesc(memberId));
  }

  /** 로그인 이력 (페이징) — 탈퇴 회원 포함 */
  @Transactional(readOnly = true)
  public PageResponse<LoginLogResponse> getLoginLogs(Long memberId, Pageable pageable) {
      memberService.getMember(memberId);   // 회원 없음(MEMBER_001)과 이력 없음(빈 페이지)을 구분

      Page<LoginLog> page = loginLogRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
      return PageResponse.of(page, LoginLogResponse::from);
  }

  /** 강제 탈퇴 — ADMIN 계정 불가(자기 자신 포함), 기존 withdraw 재사용 (soft delete + 세션 무효화) */
  @Transactional
  public void forceWithdraw(Long targetMemberId) {
      Member target = memberService.getMember(targetMemberId);

      if (target.getRole() == Role.ADMIN) {
          throw new MemberException(MemberErrorCode.CANNOT_WITHDRAW_ADMIN);
      }
      memberService.withdraw(targetMemberId);
  }

  /** 계정 복구 — soft delete 역연산. 유니크 제약 유지 정책 덕에 이메일/닉네임 충돌 불가 */
  @Transactional
  public AdminMemberResponse restore(Long memberId) {
      Member member = memberService.getMember(memberId);
      member.restore();   // 탈퇴 상태가 아니면 MEMBER_006
      return AdminMemberResponse.from(member);
  }
}
