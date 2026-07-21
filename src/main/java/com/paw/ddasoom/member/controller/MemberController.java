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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "회원(Member)", description = "내 정보 조회·수정, 로그인 이력, 비밀번호 변경, 회원 탈퇴 등 본인 회원 API")
@SecurityRequirement(name = "bearerAuth")   // 이 컨트롤러 전체가 인증 필요 (Authorize에 AT 입력)
@RequestMapping("/api/members")
public class MemberController {

  private final MemberService memberService;
  private final LoginService loginService;
  private final CookieUtil cookieUtil;


  /** 소셜 가입자(GUEST) 추가정보 입력 → USER 승급. 인가: hasRole("GUEST") — SecurityConfig */
  @Operation(summary = "소셜 가입 추가정보 입력 (GUEST → USER 승급)", description = """
            소셜 로그인으로 가입한 GUEST가 닉네임·전화번호 등 추가정보를 입력해 정회원(USER)으로 승급합니다.
            - 인가: GUEST 전용 (SecurityConfig hasRole GUEST)
            - 승급 후에는 이 API 호출 불가(이미 USER)""")
  @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "승급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패(INVALID_INPUT)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "GUEST 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "닉네임 중복(MEMBER_002)")
  })
  @PatchMapping("/me/signup-complete")
  public ResponseEntity<ApiResponse<MemberResponse>> completeSignup(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody SocialExtraInfoRequest request) {

      MemberResponse response = memberService.completeSignup(userDetails.getMemberId(), request);
      return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", response));
  }

  /** 내 정보 조회. 인가: USER/ADMIN (SecurityConfig) */
  @Operation(summary = "내 정보 조회", description = "로그인한 본인의 회원 정보를 조회합니다. 인가: USER/ADMIN.")
  @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음 또는 탈퇴(MEMBER_001/003)")
  })
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
          @AuthenticationPrincipal CustomUserDetails userDetails) {
      return ResponseEntity.ok(ApiResponse.success(memberService.getMyInfo(userDetails.getMemberId())));
  }

  /** 내 로그인 이력 최근 5건 미리보기 */
  @Operation(summary = "내 로그인 이력 최근 5건", description = "마이페이지 미리보기용. 최신순 5건을 반환합니다.")
  @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증")
  })
  @GetMapping("/me/login-logs/recent")
  public ResponseEntity<ApiResponse<List<LoginLogResponse>>> getMyRecentLoginLogs(
          @AuthenticationPrincipal CustomUserDetails userDetails) {
      return ResponseEntity.ok(ApiResponse.success(memberService.getMyRecentLoginLogs(userDetails.getMemberId())));
  }

  /** 내 로그인 이력 전체 (페이징) */
  @Operation(summary = "내 로그인 이력 전체 (페이징)", description = "전체 로그인 이력을 페이지 단위로 조회합니다. 기본 size=20, 최신순.")
  @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증")
  })
  @GetMapping("/me/login-logs")
  public ResponseEntity<ApiResponse<PageResponse<LoginLogResponse>>> getMyLoginLogs(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @RequestParam(name = "page", defaultValue = "0") int page,
          @RequestParam(name = "size", defaultValue = "20") int size) {
      return ResponseEntity.ok(ApiResponse.success(
              memberService.getMyLoginLogs(userDetails.getMemberId(), PageRequest.of(page, size))));
  }


  /** 프로필(닉네임/전화번호) 수정 */
  @Operation(summary = "프로필 수정", description = """
            닉네임·전화번호를 수정합니다.
            - 닉네임 변경 시 중복 검사(MEMBER_002)
            - 인가: USER/ADMIN""")
  @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "닉네임 중복(MEMBER_002)")
  })
  @PatchMapping("/me")
  public ResponseEntity<ApiResponse<MemberResponse>> updateProfile(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody MemberUpdateRequest request) {
      MemberResponse response = memberService.updateProfile(userDetails.getMemberId(), request);
      return ResponseEntity.ok(ApiResponse.success("회원 정보가 수정되었습니다.", response));
  }

  /** 비밀번호 변경 — 성공 시 재로그인 필요 (RT 무효화) */
  @Operation(summary = "비밀번호 변경", description = """
            현재 비밀번호 확인 후 새 비밀번호로 변경합니다.
            - 성공 시 전 세션(RT) 무효화 → 재로그인 필요
            - 소셜 전용 계정(비밀번호 없음)은 사용 불가(MEMBER_005)""")
  @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공 (재로그인 필요)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "현재 비밀번호 불일치(MEMBER_004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "소셜 전용 계정(MEMBER_005)")
  })
  @PatchMapping("/me/password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody PasswordChangeRequest request) {
      memberService.changePassword(userDetails.getMemberId(), request);
      return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다. 다시 로그인해 주세요."));
  }

  /** 회원 탈퇴 (soft delete) — 성공 시 즉시 로그아웃 처리 */
  @Operation(summary = "회원 탈퇴 (soft delete)", description = """
            본인 계정을 탈퇴 처리합니다.
            - soft delete (deleted_at 세팅) — 데이터는 유지, 로그인·조회에서 제외
            - 성공 시 즉시 로그아웃(현재 AT 블랙리스트 + RT 쿠키 삭제)
            - 탈퇴 이메일은 재가입 불가""")
  @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 성공 (로그아웃 처리됨)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "미인증")
  })
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
