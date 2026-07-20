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

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  /** 금일 신규 가입자 수 */
  @GetMapping("/members/new-today")
  public ResponseEntity<ApiResponse<NewMemberCountResponse>> getTodayNewMemberCount() {
      return ResponseEntity.ok(ApiResponse.success(dashboardService.getTodayNewMemberCount()));
  }

  // ── 이하 foster 담당: 신청 현황 요약 엔드포인트 이어서 작성 ──
  /** 처리대기 그룹 — 심사대기 / 만료임박(긴급 D-7 · 예정 D-8~30) / 답변대기 QnA */
  @GetMapping("/pending-summary")
  public ResponseEntity<ApiResponse<PendingSummaryResponse>> getPendingSummary() {
      return ResponseEntity.ok(ApiResponse.success(dashboardService.getPendingSummary()));
  }

  /** 임보 상태 5종 현재 분포 (세로 막대차트) — 배지/막대 클릭 시 상태별 목록 이동은 프론트 라우팅 */
  @GetMapping("/fosters/status-distribution")
  public ResponseEntity<ApiResponse<List<FosterStatusCountResponse>>> getFosterStatusDistribution() {
      return ResponseEntity.ok(ApiResponse.success(dashboardService.getFosterStatusDistribution()));
  }
}
