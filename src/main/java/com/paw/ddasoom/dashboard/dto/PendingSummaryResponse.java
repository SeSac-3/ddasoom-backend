package com.paw.ddasoom.dashboard.dto;

/**
 * 대시보드 "처리대기 그룹" 카드 — 심사대기 + 만료임박(긴급/예정 배타 구간) + 답변대기 QnA.
 * 각 숫자는 클릭 시 해당 처리 목록으로 이동(프론트 라우팅) — "보기만 하는 대시보드 지양" 원칙.
 */
public record PendingSummaryResponse(
        long reviewPending,     // 심사대기 (foster PENDING)
        long expiringUrgent,    // 만료 긴급 (D-0 ~ D-7)
        long expiringUpcoming,  // 만료 예정 (D-8 ~ D-30) — 긴급과 배타 구간 (이중 카운트 방지)
        long qnaPending         // 답변대기 QnA
) {}