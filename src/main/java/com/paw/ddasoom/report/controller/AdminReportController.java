package com.paw.ddasoom.report.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.report.domain.ReportStatus;
import com.paw.ddasoom.report.domain.ReportTargetType;
import com.paw.ddasoom.report.dto.response.ReportDetailResponse;
import com.paw.ddasoom.report.dto.response.ReportSummaryResponse;
import com.paw.ddasoom.report.service.ReportService;

import lombok.RequiredArgsConstructor;

/*
 * [관리자용 신고 API]
 * 유저는 접수만, 관리자는 큐 조회·판정을 담당 (ReportController와 컨트롤러만 분리, 서비스는 공유)
 * /api/admin/** 는 SecurityConfig가 URL 레벨에서 자동 잠그므로 컨트롤러에 권한 체크 코드가 없음
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

  private final ReportService reportService;

  // 1. 신고 목록 조회 (status/targetType 필터 optional, 기본 최신순)
  @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportSummaryResponse>>> getReports(
        @RequestParam(value = "status", required = false) ReportStatus status,
        @RequestParam(value = "targetType", required = false) ReportTargetType targetType,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
      return ResponseEntity.ok(ApiResponse.success("신고 목록 조회 성공",
          reportService.getReports(status, targetType, pageable)));
    }

    // 2. 신고 상세 조회
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportDetailResponse>> getReport(@PathVariable("reportId") Long reportId) {
      return ResponseEntity.ok(ApiResponse.success("신고 상세 조회 성공",
          reportService.getReport(reportId)));
    }

    // 3. 신고 승인 (→ 대상 숨김까지 수행)
    // 상태 일부만 바꾸는 부분 변경이므로 PUT이 아닌 PATCH
    @PatchMapping("/{reportId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable("reportId") Long reportId) {
      reportService.approveReport(userDetails.getMemberId(), reportId);
      return ResponseEntity.ok(ApiResponse.success("신고가 승인 처리되었습니다."));
    }

    // 4. 신고 반려 (판정만 수행 — 대상은 건드리지 않음)
    @PatchMapping("/{reportId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable("reportId") Long reportId) {
      reportService.rejectReport(userDetails.getMemberId(), reportId);
      return ResponseEntity.ok(ApiResponse.success("신고가 반려 처리되었습니다."));
    }
}