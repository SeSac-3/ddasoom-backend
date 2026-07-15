package com.paw.ddasoom.event.domain;

import java.time.LocalDate;

public enum EventPeriod {

  CONSTITUTION_DAY_EVENT(LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)),
  LIBERATION_DAY_EVENT(LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 15)),
  JULY_EVENT(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
  AUGUST_EVENT(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

  private final LocalDate startDate;
  private final LocalDate endDate;

  EventPeriod(LocalDate startDate, LocalDate endDate) {
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public boolean isActive(LocalDate date) {
    return !date.isBefore(startDate) && !date.isAfter(endDate);
  }
}