package com.paw.ddasoom.member.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.auth.domain.LoginLog;
import com.paw.ddasoom.auth.repository.LoginLogRepository;
import com.paw.ddasoom.auth.service.RedisTokenService;
import com.paw.ddasoom.auth.util.JwtUtil;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.MemberStatus;
import com.paw.ddasoom.member.domain.Role;
import com.paw.ddasoom.member.dto.request.MemberStatusUpdateRequest;
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
  private final RedisTokenService redisTokenService;
  private final JwtUtil jwtUtil;


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
      // 탈퇴 회원 포함 조회 — 관리자는 탈퇴 회원의 상세도 확인할 수 있어야 함 (복구 판단 근거)
      Member member = memberService.getMemberIncludingDeleted(memberId);
      return AdminMemberDetailResponse.of(
              member,
              memberSocialRepository.findByMemberId(memberId),
              loginLogRepository.findTop5ByMemberIdOrderByCreatedAtDesc(memberId));
  }

  /** 로그인 이력 (페이징) — 탈퇴 회원 포함 */
  @Transactional(readOnly = true)
  public PageResponse<LoginLogResponse> getLoginLogs(Long memberId, Pageable pageable) {
      // 탈퇴 회원 포함 — 제재/복구 판단 시 탈퇴 회원의 이력도 조회 대상
      memberService.getMemberIncludingDeleted(memberId);   // 회원 없음(MEMBER_001)과 이력 없음(빈 페이지)을 구분

      Page<LoginLog> page = loginLogRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
      return PageResponse.of(page, LoginLogResponse::from);
  }

  /** 강제 탈퇴 — ADMIN 계정 불가(자기 자신 포함). withdraw가 soft delete + RT 삭제 + AT 마커까지 한 세트로 처리 */
  @Transactional
  public void forceWithdraw(Long targetMemberId) {
      Member target = memberService.getMember(targetMemberId);

      if (target.getRole() == Role.ADMIN) {
          throw new MemberException(MemberErrorCode.CANNOT_WITHDRAW_ADMIN);
      }
      memberService.withdraw(targetMemberId);
  }

  /** 계정 복구 — soft delete 역연산 (DB deletedAt + Redis 차단 마커까지가 한 세트) */
  @Transactional
  public AdminMemberResponse restore(Long memberId) {
      // 복구는 "탈퇴 회원을 조회해야만 하는" 기능 — 활성 검사(getMember)를 쓰면 복구 자체가 불가능해짐.
      // 활성 회원 복구 시도의 거절은 member.restore() 내부 검사가 담당
      Member member = memberService.getMemberIncludingDeleted(memberId);
      member.restore();   // 탈퇴 상태가 아니면 MEMBER_006
      redisTokenService.clearForceLogout(memberId);   // 복구 = 차단 해제까지
      return AdminMemberResponse.from(member);
  }

  /**
   * 회원 노출 상태 변경 (신고 제재) — 관리자가 신고 내용 확인 후 직접 전환 (자동 처리 없음).
   * 탈퇴 회원은 getMember(활성 조회)가 MEMBER_003으로 차단 — 이미 탈퇴한 회원은 제재 대상이 아님.
   */
  @Transactional
  public AdminMemberResponse changeStatus(Long targetMemberId, MemberStatusUpdateRequest request) {
      Member target = memberService.getMember(targetMemberId);

      // 강제 탈퇴와 동일 정책 — 관리자 계정(자기 자신 포함)은 제재 대상 불가
      if (target.getRole() == Role.ADMIN) {
          throw new MemberException(MemberErrorCode.CANNOT_CHANGE_ADMIN_STATUS);
      }

      target.changeStatus(request.getStatus());
      return AdminMemberResponse.from(target);
  }

  // 신고 승인 시 회원 숨김(HIDDEN) 처리 — 관리자 수동 status 변경과 동일 도메인 경로 재사용
@Transactional
public void hideMember(Long targetMemberId) { 
    Member target = memberRepository.findById(targetMemberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    if (target.isDeleted() || target.getStatus() == MemberStatus.HIDDEN) { // 이미 탈퇴/숨김이면 no-op
        return;
    }
    if (target.getRole() == Role.ADMIN) { // 관리자 계정은 제재 대상 불가
        throw new MemberException(MemberErrorCode.CANNOT_CHANGE_ADMIN_STATUS);
    }
    target.changeStatus(MemberStatus.HIDDEN);
}
}
