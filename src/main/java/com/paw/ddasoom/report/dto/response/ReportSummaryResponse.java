package com.paw.ddasoom.report.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.report.domain.Report;
import com.paw.ddasoom.report.domain.ReportReason;
import com.paw.ddasoom.report.domain.ReportStatus;
import com.paw.ddasoom.report.domain.ReportTargetType;

import lombok.Builder;
import lombok.Getter;

/*
 * [신고 목록용 응답]
 * 정적 팩토리 from()에서 조립 — 엔티티를 컨트롤러 밖으로 노출하지 않음
 * 목록에서는 상세 사유(content)·처리자 정보를 싣지 않아 페이지 조회 비용을 낮춤
 */
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
  // 목록에서 상습 신고 대상 식별 + targetId 숫자만으로는 무슨 글인지 알 수 없는 문제 해결. V14 이전 데이터는 null.
  private String reportedNickname;
  private String targetTitle;

  public static ReportSummaryResponse from(Report report) {
    Member reported = report.getReportedMember();
    return ReportSummaryResponse.builder()
      .reportId(report.getReportId())
      .targetType(report.getTargetType())
      .targetId(report.getTargetId())
      .reason(report.getReason())
      .status(report.getStatus())
      .reporterNickname(report.getReporter().getNickname())
      .createdAt(report.getCreatedAt())
      // 피신고자 닉네임·대상 제목 스냅샷 (null 가드)
      .reportedNickname(reported != null ? reported.getNickname() : null)
      .targetTitle(report.getTargetTitle())
      .build();
  }
}
