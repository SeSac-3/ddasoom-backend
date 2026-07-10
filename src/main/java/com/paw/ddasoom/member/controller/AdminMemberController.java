package com.paw.ddasoom.member.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.member.domain.Role;
import com.paw.ddasoom.member.dto.response.AdminMemberDetailResponse;
import com.paw.ddasoom.member.dto.response.AdminMemberResponse;
import com.paw.ddasoom.member.dto.response.LoginLogResponse;
import com.paw.ddasoom.member.service.AdminMemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")   // /api/admin 하위 = SecurityConfig가 자동 ADMIN 잠금
public class AdminMemberController {

  private final AdminMemberService adminMemberService;

  /** 회원 목록 — keyword(이메일/닉네임 부분일치), role 필터, 가입일 최신순 */
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<AdminMemberResponse>>> getMembers(
          @RequestParam(required = false) String keyword,
          @RequestParam(required = false) Role role,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "10") int size) {

      return ResponseEntity.ok(ApiResponse.success(
              adminMemberService.getMembers(keyword, role, PageRequest.of(page, size))));
  }

  /** 회원 상세 — 기본정보 + 소셜 연동 + 최근 로그인 이력 5건 */
  @GetMapping("/{memberId}")
  public ResponseEntity<ApiResponse<AdminMemberDetailResponse>> getMemberDetail(
          @PathVariable Long memberId) {
      return ResponseEntity.ok(ApiResponse.success(adminMemberService.getMemberDetail(memberId)));
  }

  /** 로그인 이력 전체 (페이징) — 상세 화면의 "전체 보기" 탭용 */
  @GetMapping("/{memberId}/login-logs")
  public ResponseEntity<ApiResponse<PageResponse<LoginLogResponse>>> getLoginLogs(
          @PathVariable Long memberId,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size) {

      return ResponseEntity.ok(ApiResponse.success(
              adminMemberService.getLoginLogs(memberId, PageRequest.of(page, size))));
  }

  /** 강제 탈퇴 — ADMIN 계정(자기 자신 포함) 불가 */
  @DeleteMapping("/{memberId}")
  public ResponseEntity<ApiResponse<Void>> forceWithdraw(@PathVariable Long memberId) {
      adminMemberService.forceWithdraw(memberId);
      return ResponseEntity.ok(ApiResponse.success("해당 회원을 강제 탈퇴 처리했습니다."));
  }

  /** 계정 복구 — 잘못된 강제탈퇴/억울한 탈퇴 구제 (1:1 문의 연계) */
  @PatchMapping("/{memberId}/restore")
  public ResponseEntity<ApiResponse<AdminMemberResponse>> restore(@PathVariable Long memberId) {
      AdminMemberResponse response = adminMemberService.restore(memberId);
      return ResponseEntity.ok(ApiResponse.success("계정이 복구되었습니다.", response));
  }
}
