package com.paw.ddasoom.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.dashboard.dto.NewMemberCountResponse;
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
  
}
