package com.paw.ddasoom.member.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.member.domain.Role;
import com.paw.ddasoom.member.dto.request.MemberStatusUpdateRequest;
import com.paw.ddasoom.member.dto.response.AdminMemberDetailResponse;
import com.paw.ddasoom.member.dto.response.AdminMemberResponse;
import com.paw.ddasoom.member.dto.response.LoginLogResponse;
import com.paw.ddasoom.member.service.AdminMemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "회원 관리(Admin-Member)", description = "관리자 전용 — 회원 목록·상세, 강제 탈퇴/복구, 노출 상태 제재 API")
@SecurityRequirement(name = "bearerAuth")   // /api/admin 하위 = ADMIN 전용 (SecurityConfig 자동 잠금)
@RequestMapping("/api/admin/members")   // /api/admin 하위 = SecurityConfig가 자동 ADMIN 잠금
public class AdminMemberController {

  private final AdminMemberService adminMemberService;

    /** 회원 목록 — keyword(이메일/닉네임 부분일치), role 필터, 가입일 최신순 */
    @Operation(summary = "회원 목록 조회", description = """
                관리자용 회원 목록. 이메일/닉네임 부분일치 검색과 역할 필터를 지원하며 가입일 최신순 정렬입니다.
                - keyword: 이메일 또는 닉네임 부분일치 (미지정 시 전체)
                - role: GUEST/USER/ADMIN 필터 (미지정 시 전체)
                - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminMemberResponse>>> getMembers(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "role", required = false) Role role,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                adminMemberService.getMembers(keyword, role, PageRequest.of(page, size))));
    }

    /** 회원 상세 — 기본정보 + 소셜 연동 + 최근 로그인 이력 5건 */
    @Operation(summary = "회원 상세 조회", description = """
                회원 기본정보 + 소셜 연동 목록 + 최근 로그인 이력 5건을 반환합니다.
                - 탈퇴 회원도 조회 가능(복구 판단용) — getMemberIncludingDeleted 사용
                - 인가: ADMIN""")
    @Parameter(name = "memberId", description = "회원 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음(MEMBER_001)")
    })
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<AdminMemberDetailResponse>> getMemberDetail(
            @PathVariable(name = "memberId") Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(adminMemberService.getMemberDetail(memberId)));
    }

    /** 로그인 이력 전체 (페이징) — 상세 화면의 "전체 보기" 탭용 */
    @Operation(summary = "회원 로그인 이력 (페이징)", description = "특정 회원의 전체 로그인 이력을 페이지 단위로 조회합니다. 상세 화면 '전체 보기'용. 기본 size=20, 최신순. 인가: ADMIN.")
    @Parameter(name = "memberId", description = "회원 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    @GetMapping("/{memberId}/login-logs")
    public ResponseEntity<ApiResponse<PageResponse<LoginLogResponse>>> getLoginLogs(
            @PathVariable(name = "memberId") Long memberId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                adminMemberService.getLoginLogs(memberId, PageRequest.of(page, size))));
    }

    /** 강제 탈퇴 — ADMIN 계정(자기 자신 포함) 불가 */
    @Operation(summary = "회원 강제 탈퇴", description = """
                관리자가 특정 회원을 강제 탈퇴 처리합니다(soft delete).
                - ADMIN 계정(자기 자신 포함)은 대상 불가(MEMBER_006) — 관리자 상호 탈퇴 방지
                - 인가: ADMIN""")
    @Parameter(name = "memberId", description = "탈퇴시킬 회원 PK", required = true, example = "5")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "강제 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "ADMIN 계정 대상 불가(MEMBER_006)")
    })
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> forceWithdraw(@PathVariable(name = "memberId") Long memberId) {
        adminMemberService.forceWithdraw(memberId);
        return ResponseEntity.ok(ApiResponse.success("해당 회원을 강제 탈퇴 처리했습니다."));
    }

    /** 계정 복구 — 잘못된 강제탈퇴/억울한 탈퇴 구제 (1:1 문의 연계) */
    @Operation(summary = "회원 계정 복구", description = """
                탈퇴 처리된 회원을 복구합니다(deleted_at 해제). 잘못된 강제탈퇴/억울한 탈퇴 구제(1:1 문의 연계)용.
                - 인가: ADMIN""")
    @Parameter(name = "memberId", description = "복구할 회원 PK", required = true, example = "5")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "복구 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원 없음")
    })
    @PatchMapping("/{memberId}/restore")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> restore(@PathVariable(name = "memberId") Long memberId) {
        AdminMemberResponse response = adminMemberService.restore(memberId);
        return ResponseEntity.ok(ApiResponse.success("계정이 복구되었습니다.", response));
    }

    /** 회원 노출 상태 변경 — 신고 확인 후 관리자 수동 제재 (ACTIVE/HIDDEN). ADMIN 계정 대상 불가 */
    @Operation(summary = "회원 노출 상태 변경 (제재)", description = """
                신고 확인 후 관리자가 회원 노출 상태를 수동 변경합니다.
                - ACTIVE(정상) ↔ HIDDEN(숨김) 전환
                - ADMIN 계정은 대상 불가(MEMBER_008)
                - 인가: ADMIN""")
    @Parameter(name = "memberId", description = "상태 변경할 회원 PK", required = true, example = "5")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태값(INVALID_INPUT)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "ADMIN 계정 대상 불가(MEMBER_008)")
    })
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> changeStatus(
            @PathVariable(name = "memberId") Long memberId,
            @Valid @RequestBody MemberStatusUpdateRequest request) {
        AdminMemberResponse response = adminMemberService.changeStatus(memberId, request);
        return ResponseEntity.ok(ApiResponse.success("회원 상태가 변경되었습니다.", response));
    }
}
