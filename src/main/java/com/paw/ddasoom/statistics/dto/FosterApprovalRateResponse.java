package com.paw.ddasoom.statistics.dto;

/**
 * 임보 승인율 — (보호중+연장+종료) / (보호중+연장+종료+거절).
 * 대기(PENDING)는 미결 건이라 분모 제외 (넣으면 숫자가 출렁임 — 통계요청 2-2).
 * approvalRate: 0~100 (%), 소수 1자리. 분모 0이면 0.0
 */
public record FosterApprovalRateResponse(long approvedCount, long rejectedCount, double approvalRate) {}