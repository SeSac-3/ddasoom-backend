package com.paw.ddasoom.statistics.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.statistics.dto.AnimalKindRatioResponse;
import com.paw.ddasoom.statistics.dto.AnimalRegionCountResponse;
import com.paw.ddasoom.statistics.dto.FosterApprovalRateResponse;
import com.paw.ddasoom.statistics.dto.FosterAvgDurationResponse;
import com.paw.ddasoom.statistics.dto.FosterMonthlyTrendResponse;
import com.paw.ddasoom.statistics.dto.MemberSignupTrendResponse;
import com.paw.ddasoom.statistics.dto.TopFosterAnimalResponse;
import com.paw.ddasoom.statistics.service.StatisticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "관리자 통계(Statistics)", description = "관리자 — 기간 기반 추세·분포 분석 API (가입/임보/동물 통계)")
@SecurityRequirement(name = "bearerAuth")   // /api/admin 하위 = ADMIN 전용
@RequestMapping("/api/admin/statistics")
public class StatisticsController {

  private final StatisticsService statisticsService;

    /** 일별 가입자 추이 (offset: 0=최근 7일, 1=그 이전 7일...) */
    @Operation(summary = "일별 신규 가입자 추이", description = """
                7일 단위 창(window)으로 일별 가입자 수를 반환합니다.
                - offset: 0=최근 7일, 1=그 이전 7일 … (과거로 이동)
                - points[].date는 LocalDate (JSON에서 [yyyy,MM,dd] 배열로 직렬화)
                - 인가: ADMIN""")
    @Parameter(name = "offset", description = "과거 방향 창 이동(0=최근 7일)", example = "0")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/members/signup/daily")
    public ResponseEntity<ApiResponse<MemberSignupTrendResponse>> getDailyTrend(
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getDailyTrend(offset)));
    }

    /** 월별 가입자 추이 (offset: 0=이번 달, 1=지난 달...) */
    @Operation(summary = "월별 신규 가입자 추이", description = "월 단위 가입자 추이. offset: 0=이번 달, 1=지난 달 … 인가: ADMIN.")
    @Parameter(name = "offset", description = "과거 방향 이동(0=이번 달)", example = "0")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/members/signup/monthly")
    public ResponseEntity<ApiResponse<MemberSignupTrendResponse>> getMonthlyTrend(
            @RequestParam(value = "offset", defaultValue = "0") int offset) {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getMonthlyTrend(offset)));
    }

    /** 월별 임보 신청 추이 — 12개월 꺾은선, 연도 드롭다운 (기본 올해) */
    @Operation(summary = "월별 임보 신청 추이", description = """
                지정 연도의 1~12월 임보 신청 건수(꺾은선용).
                - year: 미지정 시 올해. 프론트 연도 드롭다운과 연동
                - 인가: ADMIN""")
    @Parameter(name = "year", description = "조회 연도(미지정 시 올해)", example = "2026")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/fosters/monthly")
    public ResponseEntity<ApiResponse<FosterMonthlyTrendResponse>> getFosterMonthlyTrend(
            @RequestParam(name = "year", required = false) Integer year) {
        int targetYear = year != null ? year : java.time.LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getFosterMonthlyTrend(targetYear)));
    }

    /** 임보 승인율 — 대기 제외 분모 */
    @Operation(summary = "임보 승인율", description = """
                승인율(%) = 승인 / (승인 + 반려). 대기(PENDING)는 미결이라 분모에서 제외합니다.
                - approvedCount/rejectedCount/approvalRate 반환 (분모 0이면 0.0)
                - 반려율은 프론트에서 100 - approvalRate로 계산
                - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/fosters/approval-rate")
    public ResponseEntity<ApiResponse<FosterApprovalRateResponse>> getFosterApprovalRate() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getFosterApprovalRate()));
    }

    /** 평균 임보 지속기간 (일) */
    @Operation(summary = "평균 임보 지속기간", description = """
                시작~종료일 기준 평균 임보 지속일수와 표본 수를 반환합니다.
                - sampleCount=0이면 집계 가능한 건 없음(프론트에서 '—' 표시)
                - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/fosters/avg-duration")
    public ResponseEntity<ApiResponse<FosterAvgDurationResponse>> getFosterAvgDuration() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getFosterAvgDuration()));
    }

    /** 종별(개/고양이) 등록 비율 — 도넛 */
    @Operation(summary = "동물 종별 등록 비율", description = "등록 동물의 종별(D=개, C=고양이) 건수. 도넛 차트용. 인가: ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/animals/kind-ratio")
    public ResponseEntity<ApiResponse<List<AnimalKindRatioResponse>>> getAnimalKindRatio() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getAnimalKindRatio()));
    }

    /** 보호지역 시/도별 분포 */
    @Operation(summary = "보호지역 시/도별 분포", description = """
            동물 location의 시/도 단위 집계(건수 내림차순).
            - location 첫 토큰(시/도)을 SUBSTRING_INDEX로 추출 (MySQL 전용 네이티브 쿼리)
            - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/animals/region-distribution")
    public ResponseEntity<ApiResponse<List<AnimalRegionCountResponse>>> getAnimalRegionDistribution() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getAnimalRegionDistribution()));
    }

    /** 임보 신청 많은 동물 TOP10 — 종 포함 */
    @Operation(summary = "임보 신청 많은 동물 TOP10", description = """
                임보 신청 건수 상위 10마리(종 포함).
                - 종별 필터는 프론트에서 kind로 처리(전체 순위 유지)
                - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping("/fosters/top-animals")
    public ResponseEntity<ApiResponse<List<TopFosterAnimalResponse>>> getTopFosterAnimals() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getTopFosterAnimals()));
    }

}
