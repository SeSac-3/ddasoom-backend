package com.paw.ddasoom.report.domain;

import java.time.LocalDateTime;

import com.paw.ddasoom.common.util.BaseTimeEntity;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.report.exception.ReportErrorCode;
import com.paw.ddasoom.report.exception.ReportException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

@Table(name = "report", uniqueConstraints = @UniqueConstraint(name = "uk_report_reporter_target", columnNames = {
    "reporter_id", "target_type", "target_id" }), indexes = {
        @Index(name = "idx_report_target", columnList = "target_type, target_id"),
        @Index(name = "idx_report_status_created", columnList = "status, deleted_at, created_at"),
        @Index(name = "idx_report_reported_member", columnList = "reported_member_id, deleted_at, created_at")
    })
public class Report extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "report_id")
  private Long reportId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id", nullable = false)
  private Member reporter; // 신고자

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 20)
  private ReportTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private Long targetId;

  @Enumerated(EnumType.STRING)
  @Column(name = "reason", nullable = false, length = 30)
  private ReportReason reason;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content; // 상세 사유 — reason이 ETC일 때만 필수 (ReportReason.contentRequired)

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ReportStatus status = ReportStatus.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_id")
  private Member processor; // 처리한 관리자 (미처리 시 null)


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_member_id")
  private Member reportedMember; // 피신고자 (대상 소유자). 스냅샷 실패 시 null

  @Column(name = "target_parent_id")
  private Long targetParentId; // 댓글 신고 시 원 게시글 PK — 프론트가 원문 URL을 조합하는 데 필요

  @Column(name = "target_title", length = 255)
  private String targetTitle;

  @Column(name = "target_snippet", length = 500)
  private String targetSnippet;

  @Column(name = "processed_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime processedAt; // 판정 시각 (미처리 시 null)

  /*
   * [Soft Delete(논리 삭제)를 위한 데이터 보존]
   * 신고 이력은 제재 판단 근거로 쓰이므로 물리 삭제 대신 삭제 시각만 기록
   */
  @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime deletedAt;

  /*
   * [안전하고 제한된 생성자 제공 — private + @Builder]
   * 생성 시 채울 값을 접수 정보 5개로 제한하고, status/processor/processedAt은 빌더에서 제외
   * 생성 경로에서 status를 세팅하는 것 자체를 차단해 '승인된 상태로 접수'되는 일이 없도록 함
   */
  @Builder
  private Report(Member reporter, ReportTargetType targetType, Long targetId, ReportReason reason, String content,
                 Member reportedMember, Long targetParentId, String targetTitle, String targetSnippet) {
    this.reporter = reporter;
    this.targetType = targetType;
    this.targetId = targetId;
    this.reason = reason;
    this.content = content;
    this.reportedMember = reportedMember;
    this.targetParentId = targetParentId;
    this.targetTitle = targetTitle;
    this.targetSnippet = targetSnippet;
  }

  // =========================================================================
  // 비즈니스 메서드 (의도가 명확한 상태 변경 행위들)
  // 무분별한 Setter를 배제하고, 객체 스스로가 자신의 상태를 변경하도록 도메인 로직 응집
  // =========================================================================

  /* 신고 승인 (판정만 담당 — 대상 숨김 실행은 서비스의 hideTarget이 수행) */
  public void approve(Member admin) {
    validatePending();
    this.status = ReportStatus.APPROVED;
    markProcessed(admin);
  }

  /* 신고 반려 (허위 판정 — 누적 신고 집계에서 제외됨) */
  public void reject(Member admin) {
    validatePending();
    this.status = ReportStatus.REJECTED;
    markProcessed(admin);
  }

  /*
   * [전이 규칙을 엔티티에 응집]
   * 'PENDING만 처리 가능'이라는 규칙을 서비스가 아닌 엔티티가 보유
   * 호출 경로(관리자 API, 배치 등)가 늘어나도 이 보호가 자동으로 따라붙음
   */
  private void validatePending() {
    if (this.status != ReportStatus.PENDING) {
      throw new ReportException(ReportErrorCode.REPORT_ALREADY_PROCESSED);
    }
  }

  /* 처리자·처리시각 기록 (승인/반려 공통) */
  private void markProcessed(Member admin) {
    this.processor = admin;
    this.processedAt = LocalDateTime.now();
  }

}
