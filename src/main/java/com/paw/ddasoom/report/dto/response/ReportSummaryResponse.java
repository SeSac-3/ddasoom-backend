package com.paw.ddasoom.report.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.report.domain.Report;
import com.paw.ddasoom.report.domain.ReportReason;
import com.paw.ddasoom.report.domain.ReportStatus;
import com.paw.ddasoom.report.domain.ReportTargetType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportSummaryResponse {

  private Long reportId;
  private ReportTargetType targetType;
  private Long targetId;
  private ReportReason reason;
  private ReportStatus status;
  private String reporterNickname;
  private LocalDateTime createdAt;

  public static ReportSummaryResponse from(Report report) {
    return ReportSummaryResponse.builder()
      .reportId(report.getReportId())
      .targetType(report.getTargetType())
      .targetId(report.getTargetId())
      .reason(report.getReason())
      .status(report.getStatus())
      .reporterNickname(report.getReporter().getNickname())
      .createdAt(report.getCreatedAt())
      .build();
  }
}
