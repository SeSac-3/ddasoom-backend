package com.paw.ddasoom.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.auth.exception.AuthException;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;
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
}
