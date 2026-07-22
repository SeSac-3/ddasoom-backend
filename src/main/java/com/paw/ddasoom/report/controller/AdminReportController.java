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
import com.paw.ddasoom.common.util.PageableSanitizer;
import com.paw.ddasoom.report.domain.ReportStatus;
import com.paw.ddasoom.report.domain.ReportTargetType;
import com.paw.ddasoom.report.dto.response.ReportDetailResponse;
import com.paw.ddasoom.report.dto.response.ReportSummaryResponse;
import com.paw.ddasoom.report.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/*
 * [관리자용 신고 API]
 * 유저는 접수만, 관리자는 큐 조회·판정을 담당 (ReportController와 컨트롤러만 분리, 서비스는 공유)
 * /api/admin/** 는 SecurityConfig가 URL 레벨에서 자동 잠그므로 컨트롤러에 권한 체크 코드가 없음
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "관리자 신고(Admin Report)", description = "관리자 — 신고 큐 목록·상세·승인·반려 API")
@SecurityRequirement(name = "bearerAuth")   // /api/admin 하위 = ADMIN 전용
public class AdminReportController {

  private final ReportService reportService;

  // 1. 신고 목록 조회 (status/targetType 필터 optional, 기본 최신순)
  @Operation(summary = "신고 목록 조회(관리자)", description = """
          신고 큐를 페이징으로 조회합니다. status/targetType 필터는 선택(미지정 시 전체), 기본 최신순.
          reportedMemberId를 지정하면 해당 회원이 받은 신고만 조회하며, 이때 status/targetType 필터는 무시됩니다(관리자 회원 상세 전용).
          응답 항목에는 피신고자 닉네임·대상 제목 스냅샷이 포함됩니다(V14 이전 데이터는 null).
          - 인가: ADMIN""")
  @Parameter(name = "status", description = "신고 상태 필터(미지정 시 전체)", example = "PENDING")
  @Parameter(name = "targetType", description = "대상 유형 필터(POST/POST_COMMENT/MEMBER, 미지정 시 전체)", example = "POST")
  @Parameter(name = "reportedMemberId", description = "피신고자 회원 PK. 지정 시 해당 회원이 받은 신고만 조회(다른 필터 무시)", example = "9")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
  })
  @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportSummaryResponse>>> getReports(
        @RequestParam(value = "status", required = false) ReportStatus status,
        @RequestParam(value = "targetType", required = false) ReportTargetType targetType,
        @RequestParam(value = "reportedMemberId", required = false) Long reportedMemberId,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
      // @PageableDefault의 sort는 "기본값"일 뿐 클라이언트가 덮어쓸 수 있어 방어가 아니다 — 여기서 화이트리스트로 강제
      Pageable safePageable = PageableSanitizer.sanitize(pageable,
              Sort.by(Sort.Direction.DESC, "createdAt"), "createdAt", "processedAt");
      return ResponseEntity.ok(ApiResponse.success("신고 목록 조회 성공",
          reportService.getReports(status, targetType, reportedMemberId, safePageable)));
    }

    // 2. 신고 상세 조회
    @Operation(summary = "신고 상세 조회(관리자)", description = """
            신고 단건을 조회합니다. 접수 시점 대상 스냅샷(피신고자·원 게시글 PK·제목·본문 발췌)과
            누적 신고 건수 2종을 함께 반환합니다 — targetReportCount(대상 기준)·reportedMemberReportCount(피신고자 기준),
            둘 다 반려(REJECTED) 제외 집계입니다.  V14 이전에 접수된 신고는 스냅샷·피신고자 필드가 모두 null입니다.
            - 인가: ADMIN""")
    @Parameter(name = "reportId", description = "신고 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고 없음(REPORT_001)")
    })
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportDetailResponse>> getReport(@PathVariable("reportId") Long reportId) {
      return ResponseEntity.ok(ApiResponse.success("신고 상세 조회 성공",
          reportService.getReport(reportId)));
    }

    // 3. 신고 승인 (→ 콘텐츠 제재 + 피신고자 강제탈퇴까지 수행)
    // 상태 일부만 바꾸는 부분 변경이므로 PUT이 아닌 PATCH
    @Operation(summary = "신고 승인", description = """
            신고를 승인 처리하며, 콘텐츠 강제삭제 + 피신고자 강제탈퇴(soft delete + RT 삭제 + AT 무효화) + 상태 전이를 함께 수행합니다.
            되돌릴 수 없습니다. 피신고자가 이미 탈퇴했거나 ADMIN이면 탈퇴는 건너뛰고 콘텐츠 제재만 수행합니다(멱등).
            - 인가: ADMIN""")
    @Parameter(name = "reportId", description = "신고 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "승인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 처리된 신고(REPORT_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고 없음(REPORT_001)")
    })
    @PatchMapping("/{reportId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable("reportId") Long reportId) {
      reportService.approveReport(userDetails.getMemberId(), reportId);
      return ResponseEntity.ok(ApiResponse.success("신고가 승인 처리되었습니다."));
    }

    // 4. 신고 반려 (판정만 수행 — 대상은 건드리지 않음)
    @Operation(summary = "신고 반려", description = """
            신고를 반려 처리합니다. 판정만 수행하고 대상은 건드리지 않습니다.
            - 인가: ADMIN""")
    @Parameter(name = "reportId", description = "신고 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "반려 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고 없음(REPORT_001)")
    })
    @PatchMapping("/{reportId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable("reportId") Long reportId) {
      reportService.rejectReport(userDetails.getMemberId(), reportId);
      return ResponseEntity.ok(ApiResponse.success("신고가 반려 처리되었습니다."));
    }
}