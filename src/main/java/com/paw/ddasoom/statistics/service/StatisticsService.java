package com.paw.ddasoom.statistics.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.animal.domain.AnimalKind;
import com.paw.ddasoom.animal.domain.QAnimal;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.foster.domain.QFoster;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.statistics.dto.AnimalKindRatioResponse;
import com.paw.ddasoom.statistics.dto.AnimalRegionCountResponse;
import com.paw.ddasoom.statistics.dto.FosterApprovalRateResponse;
import com.paw.ddasoom.statistics.dto.FosterAvgDurationResponse;
import com.paw.ddasoom.statistics.dto.FosterMonthlyTrendResponse;
import com.paw.ddasoom.statistics.dto.FosterMonthlyTrendResponse.MonthPoint;
import com.paw.ddasoom.statistics.dto.MemberSignupTrendResponse;
import com.paw.ddasoom.statistics.dto.MemberSignupTrendResponse.TrendPoint;
import com.paw.ddasoom.statistics.dto.TopFosterAnimalResponse;
import com.paw.ddasoom.statistics.repository.StatisticsQueryRepository;
import com.querydsl.core.Tuple;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsService {

  private final MemberRepository memberRepository;
  private final StatisticsQueryRepository statisticsQueryRepository;

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

  /** 월별 임보 신청 추이 — 12개월 한 화면(0건 월 포함), 연도 드롭다운 전환 */
  @Transactional(readOnly = true)
  public FosterMonthlyTrendResponse getFosterMonthlyTrend(int year) {
      LocalDateTime yearStart = LocalDate.of(year, 1, 1).atStartOfDay();
      Map<Integer, Long> byMonth = statisticsQueryRepository
              .countMonthlyFosters(yearStart, yearStart.plusYears(1))
              .stream()
              .collect(Collectors.toMap(
                      t -> t.get(0, Integer.class),
                      t -> t.get(1, Long.class)));

      List<MonthPoint> points = IntStream.rangeClosed(1, 12)
              .mapToObj(m -> new MonthPoint(m, byMonth.getOrDefault(m, 0L)))
              .toList();
      return new FosterMonthlyTrendResponse(year, points);
  }

  /** 임보 승인율 — 대기 제외 분모 (통계요청 2-2 계산식) */
  @Transactional(readOnly = true)
  public FosterApprovalRateResponse getFosterApprovalRate() {
      Map<FosterStatus, Long> counts = statisticsQueryRepository.countFostersByStatus();
      long approved = counts.getOrDefault(FosterStatus.FOSTERING, 0L)
              + counts.getOrDefault(FosterStatus.EXTENDED, 0L)
              + counts.getOrDefault(FosterStatus.ENDED, 0L);
      long rejected = counts.getOrDefault(FosterStatus.REJECTED, 0L);
      long denominator = approved + rejected;

      double rate = denominator == 0 ? 0.0
              : Math.round(approved * 1000.0 / denominator) / 10.0;   // % 소수 1자리
      return new FosterApprovalRateResponse(approved, rejected, rate);
  }

  /** 평균 임보 지속기간 — 일수 차이의 평균 (심사 소요는 더미 타임스탬프 조건 미충족으로 스코프 제외) */
  @Transactional(readOnly = true)
  public FosterAvgDurationResponse getFosterAvgDuration() {
      List<Tuple> durations = statisticsQueryRepository.findFosterDurations();
      if (durations.isEmpty()) {
          return new FosterAvgDurationResponse(0.0, 0);
      }
      double avgDays = durations.stream()
              .mapToLong(t -> Duration.between(
                      t.get(0, LocalDateTime.class), t.get(1, LocalDateTime.class)).toDays())
              .average()
              .orElse(0.0);
      return new FosterAvgDurationResponse(Math.round(avgDays * 10) / 10.0, durations.size());
  }

  /** 종별(개/고양이) 비율 — 0건 종도 포함 2분류 고정 */
  @Transactional(readOnly = true)
  public List<AnimalKindRatioResponse> getAnimalKindRatio() {
      Map<AnimalKind, Long> byKind = statisticsQueryRepository.countAnimalsByKind()
              .stream()
              .collect(Collectors.toMap(
                      t -> t.get(0, AnimalKind.class),
                      t -> t.get(1, Long.class)));
      return Arrays.stream(AnimalKind.values())
              .map(kind -> new AnimalKindRatioResponse(kind, byKind.getOrDefault(kind, 0L)))
              .toList();
  }

  /** 보호지역 시/도별 분포 — 건수 내림차순 */
  @Transactional(readOnly = true)
  public List<AnimalRegionCountResponse> getAnimalRegionDistribution() {
      return statisticsQueryRepository.countAnimalsByRegion()
              .stream()
              .map(row -> new AnimalRegionCountResponse(
                      (String) row[0], ((Number) row[1]).longValue()))
              .toList();
  }

  /** 임보 신청 많은 동물 TOP10 */
  @Transactional(readOnly = true)
  public List<TopFosterAnimalResponse> getTopFosterAnimals() {
      QFoster foster = QFoster.foster;
      QAnimal animal = QAnimal.animal;
      return statisticsQueryRepository.findTopFosterAnimals(10)
              .stream()
              .map(t -> new TopFosterAnimalResponse(
                      t.get(animal.id), t.get(animal.nickname), t.get(animal.abandonmentId),
                      t.get(animal.kind), t.get(animal.typeName), t.get(animal.imageUrl),
                      Optional.ofNullable(t.get(foster.count())).orElse(0L)))
              .toList();
  }

}
