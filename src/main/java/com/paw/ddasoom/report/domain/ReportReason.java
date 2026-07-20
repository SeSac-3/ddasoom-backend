package com.paw.ddasoom.report.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
  SPAM(false),                // 스팸/광고
  ABUSE(false),              // 욕설/비방
  INAPPROPRIATE(false),      // 부적절한 내용
  FRAUD(false),              // 사기/허위
  ETC(true);                 // 기타 — 상세 내용(content) 필수

  private final boolean contentRequired;
}
