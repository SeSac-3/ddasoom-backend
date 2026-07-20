package com.paw.ddasoom.report.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.report.dto.request.ReportCreateRequest;
import com.paw.ddasoom.report.service.ReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  // 로그인 필수 — PUBLIC_URIS 미등록 = anyRequest authenticated가 커버 (기본 잠금)
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> createReport(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ReportCreateRequest request) {
    reportService.createReport(userDetails.getMemberId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("신고가 접수되었습니다."));
  }
}
