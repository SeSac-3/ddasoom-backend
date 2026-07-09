package com.paw.ddasoom.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.auth.exception.AuthException;
import com.paw.ddasoom.auth.service.RedisTokenService;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;
import com.paw.ddasoom.member.dto.request.MemberUpdateRequest;
import com.paw.ddasoom.member.dto.request.PasswordChangeRequest;
import com.paw.ddasoom.member.dto.request.SocialExtraInfoRequest;
import com.paw.ddasoom.member.dto.response.MemberResponse;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

  private final MemberRepository memberRepository;
  private final RedisTokenService redisTokenService;
  private final PasswordEncoder passwordEncoder;


  /**
   * 소셜 가입자(GUEST) 추가정보 입력 → USER 승급.
   * 인가(GUEST만 접근)는 SecurityConfig가 담당하지만, 상태 검증은 서비스에서 한 번 더 —
   * 시큐리티 규칙이 실수로 풀려도 이미 USER인 회원의 재호출은 막혀야 한다.
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

  /** 조회 — 없으면 예외 (컨벤션: get = throw, find = Optional) */
  @Transactional(readOnly = true)
  public Member getMember(Long memberId) {
      return memberRepository.findById(memberId)
              .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
  }

    /** 내 정보 조회 — 마이페이지/헤더용 */
  @Transactional(readOnly = true)
  public MemberResponse getMyInfo(Long memberId) {
      return MemberResponse.from(getMember(memberId));
  }

  /** 프로필 수정 — 닉네임이 실제로 바뀌는 경우에만 중복 검사 (본인 닉네임 유지 제출 허용) */
  @Transactional
  public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
      Member member = getMember(memberId);

      boolean isNicknameChanged = !request.getNickname().equals(member.getNickname());
      if (isNicknameChanged && memberRepository.existsByNickname(request.getNickname())) {
          throw new AuthException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
      }

      member.updateProfile(request.getNickname(), request.getTel());
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

      // 비밀번호 변경 = 자격증명 교체 → 기존 세션 전부 무효화 (탈취 의심 시나리오 방어)
      redisTokenService.deleteRefreshTokens(memberId);
  }

  /**
   * 회원 탈퇴 — soft delete + 전 세션 무효화.
   * 정책(팀 결정 A): 탈퇴 이메일 재가입 불가 (soft delete + uk_member_email 유지가 곧 정책)
   * AT 블랙리스트는 컨트롤러가 헤더에서 추출해 전달 (LoginService.logout과 동일 구조)
   */
  @Transactional
  public void withdraw(Long memberId) {
      Member member = getMember(memberId);
      member.softDelete();   // 이미 탈퇴 상태면 엔티티가 ALREADY_DELETED_MEMBER throw

      // 세션 정리 ①: RT + grace 삭제 → 이후 reissue 전부 차단
      redisTokenService.deleteRefreshTokens(memberId);
  }

}
