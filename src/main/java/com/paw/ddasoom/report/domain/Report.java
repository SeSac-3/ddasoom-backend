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
        @Index(name = "idx_report_status_created", columnList = "status, deleted_at, created_at")
    })
public class Report extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "report_id")
  private Long reportId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id", nullable = false)
  private Member reporter;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 20)
  private ReportTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private Long targetId;

  @Enumerated(EnumType.STRING)
  @Column(name = "reason", nullable = false, length = 30)
  private ReportReason reason;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ReportStatus status = ReportStatus.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_id")
  private Member processor;

  @Column(name = "processed_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime processedAt;

  @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
  private LocalDateTime deletedAt;

  @Builder
  private Report(Member reporter, ReportTargetType targetType, Long targetId, ReportReason reason, String content) {
    this.reporter = reporter;
    this.targetType = targetType;
    this.targetId = targetId;
    this.reason = reason;
    this.content = content;
  }

  public void approve(Member admin) {
    validatePending();
    this.status = ReportStatus.APPROVED;
    markProcessed(admin);
  }

  public void reject(Member admin) {
    validatePending();
    this.status = ReportStatus.REJECTED;
    markProcessed(admin);
  }

  private void validatePending() {
    if (this.status != ReportStatus.PENDING) {
      throw new ReportException(ReportErrorCode.REPORT_ALREADY_PROCESSED);
    }
  }

  private void markProcessed(Member admin) {
    this.processor = admin;
    this.processedAt = LocalDateTime.now();
  }

}
