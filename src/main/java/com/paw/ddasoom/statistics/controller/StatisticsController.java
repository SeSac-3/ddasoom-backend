package com.paw.ddasoom.statistics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.statistics.dto.MemberSignupTrendResponse;
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

}
