package com.paw.ddasoom.member.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.auth.domain.LoginLog;
import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.auth.exception.AuthException;
import com.paw.ddasoom.auth.repository.LoginLogRepository;
import com.paw.ddasoom.auth.service.RedisTokenService;
import com.paw.ddasoom.auth.util.JwtUtil;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;
import com.paw.ddasoom.member.dto.request.MemberUpdateRequest;
import com.paw.ddasoom.member.dto.request.PasswordChangeRequest;
import com.paw.ddasoom.member.dto.request.SocialExtraInfoRequest;
import com.paw.ddasoom.member.dto.response.LoginLogResponse;
import com.paw.ddasoom.member.dto.response.MemberResponse;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * 본인 회원 기능 — 추가정보 승급, 내 정보 조회/수정, 로그인 이력, 비밀번호 변경, 탈퇴.
 *
 * <p>이 서비스의 모든 메서드는 memberId를 <b>파라미터로</b> 받는다(SecurityContext 직접 의존 금지 —
 * 테스트 가능성 + 비동기 스레드 안전성, 컨벤션 6). 신원은 컨트롤러가 토큰에서 추출해 넘긴다.
 *
 * <p>조회의 두 경로를 명확히 구분한다:
 * getMember(활성만, 탈퇴면 예외) vs getMemberIncludingDeleted(탈퇴 포함, 관리자 전용).
 * 일반 로직이 전자를 쓰기만 하면 "탈퇴 회원 대상 작업"이 구조적으로 차단된다.
 */
@Service
@RequiredArgsConstructor
public class MemberService {

  private final MemberRepository memberRepository;
  private final RedisTokenService redisTokenService;
  private final PasswordEncoder passwordEncoder;
  private final LoginLogRepository loginLogRepository;
  private final JwtUtil jwtUtil; 


  /**
   * 소셜 가입자(GUEST) 추가정보 입력 → USER 승급.
   * 인가(GUEST만 접근)는 SecurityConfig가 담당하지만, 상태 검증은 서비스에서 한 번 더 —
   * 시큐리티 규칙이 실수로 풀려도 이미 USER인 회원의 재호출은 막혀야 한다(방어적 이중 검증).
   */
  @Transactional
  public MemberResponse completeSignup(Long memberId, SocialExtraInfoRequest request) {
      Member member = getMember(memberId);

      if (member.getRole() != Role.GUEST) {
          throw new MemberException(MemberErrorCode.ALREADY_USER_ROLE);
      }
      // 닉네임 중복 검사 — 일반 가입(AuthService.signup)과 동일한 코드로 응답 통일
      if (memberRepository.existsByNickname(request.getNickname())) {
          throw new AuthException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
      }

      member.updateExtraInfo(request.getName(), request.getNickname(), request.getTel());
      return MemberResponse.from(member);   // 더티 체킹으로 UPDATE — save() 불필요
  }

  /**
   * 활성 회원 조회 — 없으면 MEMBER_001, 탈퇴(soft delete) 상태면 MEMBER_003.
   * 일반 서비스 로직의 기본 조회 경로: 이걸 쓰는 모든 곳이 "탈퇴 회원 대상 작업"을 자동 차단한다.
   * (신고 제재로 탈퇴 처리된 회원이 어떤 경로로든 대상이 되는 것을 원천 방지 — A-5 연계)
   */
  public Member getMember(Long memberId) {
      Member member = memberRepository.findById(memberId)
              .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
      if (member.isDeleted()) {
          throw new MemberException(MemberErrorCode.ALREADY_DELETED_MEMBER);
      }
      return member;
  }

  /**
   * 탈퇴 회원 포함 조회 — 없으면 MEMBER_001만.
   * 탈퇴 회원을 다뤄야만 하는 관리자 기능(상세 조회·로그인 이력·복구) 전용.
   * ⚠️ 일반 서비스 로직에서 사용 금지 — 탈퇴 검사가 우회된다.
   */
  public Member getMemberIncludingDeleted(Long memberId) {
      return memberRepository.findById(memberId)
              .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
  }

    /** 내 정보 조회 — 마이페이지/헤더용 */
  @Transactional(readOnly = true)
  public MemberResponse getMyInfo(Long memberId) {
      return MemberResponse.from(getMember(memberId));
  }

  /** 최근 로그인 이력 5건 — 마이페이지 미리보기. "낯선 로그인" 자가 점검용 */
  @Transactional(readOnly = true)
  public List<LoginLogResponse> getMyRecentLoginLogs(Long memberId) {
        return loginLogRepository.findTop5ByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(LoginLogResponse::from)
                .toList();
    }

  /** 로그인 이력 전체(페이징) — 미리보기 5건 외 과거 이력까지 */
  @Transactional(readOnly = true)
  public PageResponse<LoginLogResponse> getMyLoginLogs(Long memberId, Pageable pageable) {
        Page<LoginLog> page = loginLogRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        return PageResponse.of(page, LoginLogResponse::from);
    }

  /** 프로필 수정 — 닉네임이 실제로 바뀌는 경우에만 중복 검사 (본인 닉네임 유지 제출 허용) */
  @Transactional
  public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
      Member member = getMember(memberId);

      // 본인이 기존 닉네임 그대로 제출한 경우까지 중복 에러를 내면 안 되므로, "실제 변경"일 때만 검사
      boolean isNicknameChanged = !request.getNickname().equals(member.getNickname());
      if (isNicknameChanged && memberRepository.existsByNickname(request.getNickname())) {
          throw new AuthException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
      }

      member.updateProfile(request.getName(), request.getNickname(), request.getTel());
      return MemberResponse.from(member);
  }

  /** 비밀번호 변경 — 성공 시 전 세션 무효화(RT 삭제) → 재로그인 필요 */
  @Transactional
  public void changePassword(Long memberId, PasswordChangeRequest request) {
      Member member = getMember(memberId);

      // 소셜 전용 회원(password=null)은 변경 대상 아님 — matches의 NPE 방지 겸 명시적 안내
      if (member.getPassword() == null) {
          throw new MemberException(MemberErrorCode.SOCIAL_MEMBER_HAS_NO_PASSWORD);
      }
      if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
          throw new MemberException(MemberErrorCode.PASSWORD_MISMATCH);
      }

      member.changePassword(passwordEncoder.encode(request.getNewPassword()));

      // 비밀번호 변경 = 자격증명 교체 → 기존 세션 전부 무효화 (탈취 의심 시나리오 방어).
      // 단 강제로그아웃 마커는 안 건다 — 탈퇴처럼 "신원 소멸"이 아니라 RT 무효화로 충분(SECURITY-FLOW 5절).
      redisTokenService.deleteRefreshTokens(memberId);
  }

 /** 회원 탈퇴 (자진·강제 공용) — soft delete + 세션 완전 정리 */
  @Transactional
  public void withdraw(Long memberId) {
      Member member = getMember(memberId);
      member.softDelete();

      // ⚠️ Redis 조작(②③)이 실패하면 @Transactional에 의해 softDelete(①)까지 롤백되는 것이 의도된 동작이다.
      //    "DB는 탈퇴인데 세션은 살아있는" 불일치 상태를 만들지 않기 위함 — 전부 성공하거나 전부 없던 일로.
      //    (Redis 조작 자체는 JPA 트랜잭션 대상이 아니지만, 예외가 나면 트랜잭션이 롤백되며 ①이 취소된다.)
      // ① RT+grace 삭제 (재발급 차단)
      redisTokenService.deleteRefreshTokens(memberId);
      // ② 이미 발급된 모든 AT 즉시 차단 — 다른 탭/기기 포함 (탈퇴 회원의 활동 창 제거)
      redisTokenService.markForceLogout(memberId, jwtUtil.getAccessTokenValidityMillis());
  }

}
