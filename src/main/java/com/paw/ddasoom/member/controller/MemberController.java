package com.paw.ddasoom.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.member.dto.request.SocialExtraInfoRequest;
import com.paw.ddasoom.member.dto.response.MemberResponse;
import com.paw.ddasoom.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

  private final MemberService memberService;

  /** 소셜 가입자(GUEST) 추가정보 입력 → USER 승급. 인가: hasRole("GUEST") — SecurityConfig */
  @PatchMapping("/me/signup-complete")
  public ResponseEntity<ApiResponse<MemberResponse>> completeSignup(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody SocialExtraInfoRequest request) {

      MemberResponse response = memberService.completeSignup(userDetails.getMemberId(), request);
      return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", response));
  }

    /** 내 정보 조회. 인가: USER/ADMIN (SecurityConfig) */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
          @AuthenticationPrincipal CustomUserDetails userDetails) {
      return ResponseEntity.ok(ApiResponse.success(memberService.getMyInfo(userDetails.getMemberId())));
  }
}
