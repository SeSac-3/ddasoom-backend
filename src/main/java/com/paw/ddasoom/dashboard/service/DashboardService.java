package com.paw.ddasoom.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.dashboard.dto.FosterStatusCountResponse;
import com.paw.ddasoom.dashboard.dto.NewMemberCountResponse;
import com.paw.ddasoom.dashboard.dto.PendingSummaryResponse;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.statistics.repository.StatisticsQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
  
  private final MemberRepository memberRepository;
  // ── 이하 foster 담당: private final FosterRepository fosterRepository; 등 이어서 주입 ──
  private final StatisticsQueryRepository statisticsQueryRepository;
  
  /** 금일 신규 가입자 수 (오늘 00:00 ~ 현재) */
  @Transactional(readOnly = true)
  public NewMemberCountResponse getTodayNewMemberCount() {
      LocalDateTime start = LocalDate.now().atStartOfDay();
      long count = memberRepository.countByCreatedAtBetween(start, LocalDateTime.now());
      return new NewMemberCountResponse(count);
  }

  // ── 이하 foster 담당: 신청 상태별 집계 메서드 이어서 작성 ──

  /** 처리대기 그룹 — 심사대기 + 만료임박(긴급/예정) + 답변대기 QnA를 1회 응답으로 */
  @Transactional(readOnly = true)
  public PendingSummaryResponse getPendingSummary() {
      // 구간 경계는 자정 기준 반개구간 — 긴급 [오늘, +8일) = D-0~D-7 / 예정 [+8일, +31일) = D-8~D-30.
      // 같은 상한/하한을 공유하므로 두 구간이 수학적으로 배타 (이중 카운트 원천 차단 — 통계요청 1-2)
      java.time.LocalDateTime todayStart = LocalDate.now().atStartOfDay();
      java.time.LocalDateTime urgentEnd = todayStart.plusDays(8);
      java.time.LocalDateTime upcomingEnd = todayStart.plusDays(31);

      Map<FosterStatus, Long> statusCounts = statisticsQueryRepository.countFostersByStatus();

      return new PendingSummaryResponse(
              statusCounts.getOrDefault(FosterStatus.PENDING, 0L),
              statisticsQueryRepository.countExpiringFosters(todayStart, urgentEnd),
              statisticsQueryRepository.countExpiringFosters(urgentEnd, upcomingEnd),
              statisticsQueryRepository.countPendingQnas());
  }

  /** 임보 상태 5종 현재 분포 (막대차트용) — 0건 상태도 포함해 enum 선언 순서로 고정 반환 */
  @Transactional(readOnly = true)
  public List<FosterStatusCountResponse> getFosterStatusDistribution() {
      Map<FosterStatus, Long> counts = statisticsQueryRepository.countFostersByStatus();
      return Arrays.stream(FosterStatus.values())
              .map(status -> new FosterStatusCountResponse(status, counts.getOrDefault(status, 0L)))
              .toList();
  }
  
}
