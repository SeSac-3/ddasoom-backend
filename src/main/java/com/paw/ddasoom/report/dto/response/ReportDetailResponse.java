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
public class ReportDetailResponse {

  private Long reportId;
  private ReportTargetType targetType;
  private Long targetId;
  private ReportReason reason;
  private String content;
  private ReportStatus status;
  private String reporterNickname;
  private String processorNickname;
  private LocalDateTime processedAt;
  private long targetReportCount; // 대상 누적 신고 건수
  private LocalDateTime createdAt;

  public static ReportDetailResponse from(Report report, long targetReportCount) {
    return ReportDetailResponse.builder()
      .reportId(report.getReportId())
      .targetType(report.getTargetType())
      .targetId(report.getTargetId())
      .reason(report.getReason())
      .content(report.getContent())
      .status(report.getStatus())
      .reporterNickname(report.getReporter().getNickname())
      .processorNickname(report.getProcessor() != null ? report.getProcessor().getNickname() : null)
      .processedAt(report.getProcessedAt())
      .targetReportCount(targetReportCount)
      .createdAt(report.getCreatedAt())
      .build();
  }

}
