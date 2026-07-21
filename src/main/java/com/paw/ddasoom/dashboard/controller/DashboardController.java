package com.paw.ddasoom.dashboard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.dashboard.dto.FosterStatusCountResponse;
import com.paw.ddasoom.dashboard.dto.NewMemberCountResponse;
import com.paw.ddasoom.dashboard.dto.PendingSummaryResponse;
import com.paw.ddasoom.dashboard.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "관리자 대시보드(Dashboard)", description = "관리자 메인 — 오늘 처리할 일(액션 지표)과 현재 스냅샷 API")
@SecurityRequirement(name = "bearerAuth")   // /api/admin 하위 = ADMIN 전용
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

    /** 금일 신규 가입자 수 */
    @Operation(summary = "오늘 신규 가입자 수", description = "금일(00:00~현재) 가입한 회원 수. 대시보드 상단 카드용. 인가: ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/members/new-today")
    public ResponseEntity<ApiResponse<NewMemberCountResponse>> getTodayNewMemberCount() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getTodayNewMemberCount()));
    }

    // ── 이하 foster 담당: 신청 현황 요약 엔드포인트 이어서 작성 ──
    /** 처리대기 그룹 — 심사대기 / 만료임박(긴급 D-7 · 예정 D-8~30) / 답변대기 QnA */
    @Operation(summary = "처리대기 요약", description = """
                관리자가 지금 처리해야 할 항목들의 건수 묶음:
                - reviewPending: 심사 대기(PENDING) 임보 신청 수
                - expiringUrgent: 만료 긴급 (오늘~D+7)
                - expiringUpcoming: 만료 예정 (D+8~D+30, 긴급과 배타 구간)
                - qnaPending: 답변 대기 QnA 수
                인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/pending-summary")
    public ResponseEntity<ApiResponse<PendingSummaryResponse>> getPendingSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getPendingSummary()));
    }

    /** 임보 상태 5종 현재 분포 (세로 막대차트) — 배지/막대 클릭 시 상태별 목록 이동은 프론트 라우팅 */
    @Operation(summary = "임보 상태 분포", description = """
                임보 신청 5종 상태(PENDING/FOSTERING/EXTENDED/ENDED/REJECTED)의 현재 건수 분포.
                - 0건 상태도 포함해 반환(막대차트 축 고정용)
                - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/fosters/status-distribution")
    public ResponseEntity<ApiResponse<List<FosterStatusCountResponse>>> getFosterStatusDistribution() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getFosterStatusDistribution()));
    }
}
