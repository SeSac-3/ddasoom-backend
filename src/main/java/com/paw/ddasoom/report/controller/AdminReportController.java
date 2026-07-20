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

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

  private final ReportService reportService;

  @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportSummaryResponse>>> getReports(
        @RequestParam(required = false) ReportStatus status,
        @RequestParam(required = false) ReportTargetType targetType,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
      return ResponseEntity.ok(ApiResponse.success("신고 목록 조회 성공",
          reportService.getReports(status, targetType, pageable)));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportDetailResponse>> getReport(@PathVariable Long reportId) {
      return ResponseEntity.ok(ApiResponse.success("신고 상세 조회 성공",
          reportService.getReport(reportId)));
    }

    @PatchMapping("/{reportId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long reportId) {
      reportService.approveReport(userDetails.getMemberId(), reportId);
      return ResponseEntity.ok(ApiResponse.success("신고가 승인 처리되었습니다."));
    }

    @PatchMapping("/{reportId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long reportId) {
      reportService.rejectReport(userDetails.getMemberId(), reportId);
      return ResponseEntity.ok(ApiResponse.success("신고가 반려 처리되었습니다."));
    }
}