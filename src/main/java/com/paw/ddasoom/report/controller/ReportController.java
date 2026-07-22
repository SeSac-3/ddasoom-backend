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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "신고(Report)", description = "사용자 — 게시글·댓글·회원 신고 접수 API")
@SecurityRequirement(name = "bearerAuth")   // 로그인 필수 (anyRequest authenticated)
public class ReportController {

  private final ReportService reportService;

  // 1. 신고 접수 — 유저는 '접수'만 가능하고, 큐 조회·판정은 AdminReportController가 담당
  // 로그인 필수 — PUBLIC_URIS 미등록 = anyRequest authenticated가 커버 (기본 잠금)
  @Operation(summary = "신고 접수", description = """
          게시글/댓글/회원을 신고 접수합니다. 자기 자신 신고·중복 신고는 차단됩니다.
          기타(ETC) 사유는 상세 내용이 필수입니다.
          - 인가: USER(로그인 사용자)""")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "접수 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "기타 사유 상세 필수(REPORT_004) / 자기 신고 불가(REPORT_005)"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고 대상 없음(REPORT_006)"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 신고한 대상(REPORT_002)")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> createReport(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ReportCreateRequest request) {
    reportService.createReport(userDetails.getMemberId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("신고가 접수되었습니다."));
  }
}
