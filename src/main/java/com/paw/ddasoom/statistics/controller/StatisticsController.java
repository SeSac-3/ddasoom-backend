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

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/statistics")
public class StatisticsController {

  private final StatisticsService statisticsService;

  /** 일별 가입자 추이 (offset: 0=최근 7일, 1=그 이전 7일...) */
  @GetMapping("/members/signup/daily")
  public ResponseEntity<ApiResponse<MemberSignupTrendResponse>> getDailyTrend(
          @RequestParam(value = "offset", defaultValue = "0") int offset) {
      return ResponseEntity.ok(ApiResponse.success(statisticsService.getDailyTrend(offset)));
  }

  /** 월별 가입자 추이 (offset: 0=이번 달, 1=지난 달...) */
  @GetMapping("/members/signup/monthly")
  public ResponseEntity<ApiResponse<MemberSignupTrendResponse>> getMonthlyTrend(
          @RequestParam(value = "offset", defaultValue = "0") int offset) {
      return ResponseEntity.ok(ApiResponse.success(statisticsService.getMonthlyTrend(offset)));
  }

  /** 월별 임보 신청 추이 — 12개월 꺾은선, 연도 드롭다운 (기본 올해) */
  @GetMapping("/fosters/monthly")
  public ResponseEntity<ApiResponse<FosterMonthlyTrendResponse>> getFosterMonthlyTrend(
          @RequestParam(name = "year", required = false) Integer year) {
      int targetYear = year != null ? year : java.time.LocalDate.now().getYear();
      return ResponseEntity.ok(ApiResponse.success(statisticsService.getFosterMonthlyTrend(targetYear)));
  }

  /** 임보 승인율 — 대기 제외 분모 */
  @GetMapping("/fosters/approval-rate")
  public ResponseEntity<ApiResponse<FosterApprovalRateResponse>> getFosterApprovalRate() {
      return ResponseEntity.ok(ApiResponse.success(statisticsService.getFosterApprovalRate()));
  }

  /** 평균 임보 지속기간 (일) */
  @GetMapping("/fosters/avg-duration")
  public ResponseEntity<ApiResponse<FosterAvgDurationResponse>> getFosterAvgDuration() {
      return ResponseEntity.ok(ApiResponse.success(statisticsService.getFosterAvgDuration()));
  }

  /** 종별(개/고양이) 등록 비율 — 도넛 */
  @GetMapping("/animals/kind-ratio")
  public ResponseEntity<ApiResponse<List<AnimalKindRatioResponse>>> getAnimalKindRatio() {
      return ResponseEntity.ok(ApiResponse.success(statisticsService.getAnimalKindRatio()));
  }

  /** 보호지역 시/도별 분포 */
  @GetMapping("/animals/region-distribution")
  public ResponseEntity<ApiResponse<List<AnimalRegionCountResponse>>> getAnimalRegionDistribution() {
      return ResponseEntity.ok(ApiResponse.success(statisticsService.getAnimalRegionDistribution()));
  }

  /** 임보 신청 많은 동물 TOP10 — 종 포함 */
  @GetMapping("/fosters/top-animals")
  public ResponseEntity<ApiResponse<List<TopFosterAnimalResponse>>> getTopFosterAnimals() {
      return ResponseEntity.ok(ApiResponse.success(statisticsService.getTopFosterAnimals()));
  }

}
