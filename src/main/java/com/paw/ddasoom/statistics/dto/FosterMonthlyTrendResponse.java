package com.paw.ddasoom.statistics.dto;

import java.util.List;

/** 월별 임보 신청 추이 — 1~12월 12포인트 고정(0건 포함), 연도는 드롭다운 전환 */
public record FosterMonthlyTrendResponse(int year, List<MonthPoint> points) {
    public record MonthPoint(int month, long count) {}
}