package com.paw.ddasoom.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.dashboard.dto.NewMemberCountResponse;
import com.paw.ddasoom.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {
  
  private final MemberRepository memberRepository;
  // ── 이하 foster 담당: private final FosterRepository fosterRepository; 등 이어서 주입 ──

  /** 금일 신규 가입자 수 (오늘 00:00 ~ 현재) */
  @Transactional(readOnly = true)
  public NewMemberCountResponse getTodayNewMemberCount() {
      LocalDateTime start = LocalDate.now().atStartOfDay();
      long count = memberRepository.countByCreatedAtBetween(start, LocalDateTime.now());
      return new NewMemberCountResponse(count);
  }

  // ── 이하 foster 담당: 신청 상태별 집계 메서드 이어서 작성 ──
  
}
