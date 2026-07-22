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
 * [신고 상세 응답]
 * 정적 팩토리 from()에서 조립 — 엔티티(Report)를 컨트롤러 밖으로 노출하지 않기 위한 경계
 * targetReportCount는 엔티티 필드가 아니라 서비스가 별도 집계해 넘겨주는 값
 */
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

  // 접수 시점 대상 스냅샷. targetId 숫자만으로는 무엇이/누가 신고당했는지 알 수 없어서 함께 노출한다.
  private Long targetParentId;              // 댓글 신고 시 원 게시글 PK (원문 URL 조합용)
  private String targetTitle;
  private String targetSnippet;
  // 피신고자 정보 + 상습성 판단 근거. V14 이전 데이터는 스냅샷이 없어 전부 null/false/0이다.
  private Long reportedMemberId;
  private String reportedNickname;
  private boolean reportedMemberWithdrawn;  // 이미 탈퇴(soft delete)됐는지 — 승인 시 강제탈퇴가 no-op이 되는 조건
  private long reportedMemberReportCount;   // 이 피신고자가 받은 누적 신고 건수(반려 제외)

  // 시그니처에 reportedMemberReportCount 추가. 피신고자 필드는 스냅샷 도입 이전 데이터를 위해 전부 null 가드.
  public static ReportDetailResponse from(Report report, long targetReportCount, long reportedMemberReportCount) {
    Member reported = report.getReportedMember();
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
      // 스냅샷 필드 매핑
      .targetParentId(report.getTargetParentId())
      .targetTitle(report.getTargetTitle())
      .targetSnippet(report.getTargetSnippet())
      // 피신고자 매핑 (reported == null이면 모두 기본값 유지)
      .reportedMemberId(reported != null ? reported.getId() : null)
      .reportedNickname(reported != null ? reported.getNickname() : null)
      .reportedMemberWithdrawn(reported != null && reported.isDeleted())
      .reportedMemberReportCount(reportedMemberReportCount)
      .build();
  }

}
