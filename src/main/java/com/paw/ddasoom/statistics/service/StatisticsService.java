package com.paw.ddasoom.statistics.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.statistics.dto.MemberSignupTrendResponse;
import com.paw.ddasoom.statistics.dto.MemberSignupTrendResponse.TrendPoint;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsService {

  private final MemberRepository memberRepository;

  /**
   * 일별 가입자 추이 — 오늘 기준 최근 7일을 한 구간으로, offset으로 과거 구간 탐색.
   * offset=0: 오늘 포함 최근 7일 / offset=1: 그 직전 7일 ...
   */
  @Transactional(readOnly = true)
  public MemberSignupTrendResponse getDailyTrend(int offset) {
      LocalDate windowEnd = LocalDate.now().minusDays(7L * offset);
      LocalDate windowStart = windowEnd.minusDays(6);

      List<TrendPoint> points = new ArrayList<>();
      for (LocalDate d = windowStart; !d.isAfter(windowEnd); d = d.plusDays(1)) {
          long count = memberRepository.countByCreatedAtBetween(d.atStartOfDay(), d.plusDays(1).atStartOfDay());
          points.add(new TrendPoint(d, count));
      }
      return new MemberSignupTrendResponse("DAY", offset, windowStart, windowEnd, points);
  }

  /**
   * 월별 가입자 추이 — 금월 기준, offset으로 과거 월 탐색.
   * offset=0: 이번 달 / offset=1: 지난 달 ... (캘린더 월 기준)
   */
  @Transactional(readOnly = true)
  public MemberSignupTrendResponse getMonthlyTrend(int offset) {
      YearMonth targetMonth = YearMonth.now().minusMonths(offset);
      LocalDate start = targetMonth.atDay(1);
      LocalDate end = targetMonth.atEndOfMonth();

      long count = memberRepository.countByCreatedAtBetween(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
      // 월별은 해당 월 총합 1포인트 (일자별 세분화가 필요하면 daily처럼 루프로 확장)
      List<TrendPoint> points = List.of(new TrendPoint(start, count));
      return new MemberSignupTrendResponse("MONTH", offset, start, end, points);
  }

}
