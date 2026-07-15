package com.paw.ddasoom.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record MemberSignupTrendResponse(
    String unit,
    int offset,
    LocalDate periodStart,
    LocalDate periodEnd,
    List<TrendPoint> points
) {
  public record TrendPoint(LocalDate date, long count) {}
}
