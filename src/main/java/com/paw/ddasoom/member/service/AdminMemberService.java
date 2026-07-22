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

/**
 * 관리자 회원 관리 — 목록·상세, 로그인 이력, 강제 탈퇴/복구, 노출 상태 제재.
 *
 * <p>MemberService의 조회/탈퇴 로직을 재사용한다(중복 구현 금지). 특히 조회는 목적에 따라
 * 활성 전용(getMember)과 탈퇴 포함(getMemberIncludingDeleted)을 의도적으로 골라 쓴다 —
 * 복구·상세는 탈퇴 회원을 봐야 하므로 후자, 제재(강제탈퇴·상태변경)는 활성 대상만이라 전자.
 *
 * <p>관리자 계정 보호: 강제 탈퇴·상태 변경 모두 ADMIN 대상은 불가(자기 자신 포함).
 * "관리자끼리 상호 제재"로 서비스가 마비되는 상황을 원천 차단한다.
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

  private final MemberRepository memberRepository;
  private final MemberSocialRepository memberSocialRepository;
  private final LoginLogRepository loginLogRepository;
  private final MemberService memberService;   // getMember, withdraw 재사용
  private final RedisTokenService redisTokenService;
  private final JwtUtil jwtUtil;


  /** 목록 — 탈퇴 회원 포함 전체. 상태 구분은 응답의 deletedAt/status로 프론트가 표시 */
  @Transactional(readOnly = true)
  public PageResponse<AdminMemberResponse> getMembers(String keyword, Role role, Pageable pageable) {
      // 빈 문자열 keyword는 null로 정규화 (JPQL 조건 미적용 — "빈 검색 = 전체"가 되게)
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
      // 존재 검증만 먼저 수행(탈퇴 포함) — "회원 없음(MEMBER_001)"과 "이력 없음(빈 페이지)"을 구분해 응답하기 위함
      memberService.getMemberIncludingDeleted(memberId);

      Page<LoginLog> page = loginLogRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
      return PageResponse.of(page, LoginLogResponse::from);
  }

  /** 강제 탈퇴 — ADMIN 계정 불가(자기 자신 포함). withdraw가 soft delete + RT 삭제 + AT 마커까지 한 세트로 처리 */
  @Transactional
  public void forceWithdraw(Long targetMemberId) {
      // 활성 조회 — 이미 탈퇴한 회원을 또 강제탈퇴하는 무의미한 호출은 getMember가 MEMBER_003으로 차단
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
      redisTokenService.clearForceLogout(memberId);   // 복구 = 차단 해제까지 (DB·Redis 양쪽 되돌리는 온전한 역연산)
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

      // 상태 전이에 맞춰 세션도 동기화 — DB와 Redis가 따로 노는 상태를 만들지 않는다.
      if (request.getStatus() == MemberStatus.HIDDEN) {
          blockSessions(targetMemberId);                        // 제재 → 즉시 차단
      } else {
          redisTokenService.clearForceLogout(targetMemberId);   // ACTIVE 복귀 → 차단 해제 (restore와 동일한 역연산)
      }
      return AdminMemberResponse.from(target);
  }

    /**
     * 신고 승인 시 회원 숨김(HIDDEN) 처리 — 신고 도메인(ReportService.approveReport)이 호출하는 진입점.
     * 관리자 수동 changeStatus와 같은 도메인 경로(Member.changeStatus)를 재사용해 "회원 상태의 진실"을
     * member 도메인 한 곳에만 둔다.
     *
     * <p>여기서 getMember(활성 조회)를 안 쓰고 findById로 직접 조회하는 이유: 신고 승인은 배치성 호출이라
     * 이미 탈퇴/숨김인 대상에 대해 예외를 던지기보다 <b>조용히 no-op</b>으로 넘어가는 게 맞다
     * (같은 신고를 두 번 승인해도 안전하도록 — 멱등성).
     */
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
        blockSessions(targetMemberId);   // 신고 승인 제재도 관리자 수동 제재와 동일 강도로 세션 차단
    }

    /**
   * 제재(HIDDEN) 적용 시 세션까지 즉시 차단 — 탈퇴(withdraw)와 동일한 세트.
   *
   * <p>상태 컬럼만 바꾸면 ① 이미 로그인된 세션이 그대로 활동하고 ② RT 슬라이딩 갱신으로 사실상
   * 무기한 유지되며 ③ 소셜 계정이면 새로 로그인까지 된다 — "제재가 로그인 폼 하나만 잠그는" 상태.
   * 팀이 탈퇴에 대해 세운 "정지된 회원은 즉시 아무것도 할 수 없어야 한다"는 원칙을 제재에도 동일 적용한다.
   * (기존 인프라 재사용이라 인증 요청당 Redis 조회 증분은 0 — 이미 지불 중인 비용)
   */
  private void blockSessions(Long memberId) {
      redisTokenService.deleteRefreshTokens(memberId);   // 재발급 경로 차단(RT + grace)
      // 기발급 AT 전부 즉시 무효화 — 다른 탭·기기 포함. TTL은 AT 최대 수명이면 충분(그 뒤엔 자연 만료)
      redisTokenService.markForceLogout(memberId, jwtUtil.getAccessTokenValidityMillis());
  }
}
