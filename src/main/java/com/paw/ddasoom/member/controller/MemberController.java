package com.paw.ddasoom.member.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.auth.service.LoginService;
import com.paw.ddasoom.auth.util.CookieUtil;
import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.member.dto.request.MemberUpdateRequest;
import com.paw.ddasoom.member.dto.request.PasswordChangeRequest;
import com.paw.ddasoom.member.dto.request.SocialExtraInfoRequest;
import com.paw.ddasoom.member.dto.response.LoginLogResponse;
import com.paw.ddasoom.member.dto.response.MemberResponse;
import com.paw.ddasoom.member.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

  private final MemberService memberService;
  private final LoginService loginService;
  private final CookieUtil cookieUtil;


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
  /** 내 로그인 이력 최근 5건 미리보기 */
  @GetMapping("/me/login-logs/recent")
  public ResponseEntity<ApiResponse<List<LoginLogResponse>>> getMyRecentLoginLogs(
          @AuthenticationPrincipal CustomUserDetails userDetails) {
      return ResponseEntity.ok(ApiResponse.success(memberService.getMyRecentLoginLogs(userDetails.getMemberId())));
  }

  /** 내 로그인 이력 전체 (페이징) */
  @GetMapping("/me/login-logs")
  public ResponseEntity<ApiResponse<PageResponse<LoginLogResponse>>> getMyLoginLogs(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestParam(name = "page", defaultValue = "0") int page,
          @RequestParam(name = "size", defaultValue = "20") int size) {
      return ResponseEntity.ok(ApiResponse.success(
              memberService.getMyLoginLogs(userDetails.getMemberId(), PageRequest.of(page, size))));
  }


  /** 프로필(닉네임/전화번호) 수정 */
  @PatchMapping("/me")
  public ResponseEntity<ApiResponse<MemberResponse>> updateProfile(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody MemberUpdateRequest request) {
      MemberResponse response = memberService.updateProfile(userDetails.getMemberId(), request);
      return ResponseEntity.ok(ApiResponse.success("회원 정보가 수정되었습니다.", response));
  }

  /** 비밀번호 변경 — 성공 시 재로그인 필요 (RT 무효화) */
  @PatchMapping("/me/password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody PasswordChangeRequest request) {
      memberService.changePassword(userDetails.getMemberId(), request);
      return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다. 다시 로그인해 주세요."));
  }

  /** 회원 탈퇴 (soft delete) — 성공 시 즉시 로그아웃 처리 */
  @DeleteMapping("/me")
  public ResponseEntity<ApiResponse<Void>> withdraw(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestHeader("Authorization") String authorizationHeader) {

      memberService.withdraw(userDetails.getMemberId());

      // 세션 정리 ②: 현재 AT 블랙리스트 + RT 쿠키 삭제 — 기존 logout 로직 재사용
      String accessToken = authorizationHeader.replaceFirst("^Bearer ", "");
      loginService.logout(userDetails.getMemberId(), accessToken);

      return ResponseEntity.ok()
              .header(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString())
              .body(ApiResponse.success("탈퇴가 완료되었습니다. 그동안 따숨과 함께해 주셔서 감사합니다."));
  }
}
