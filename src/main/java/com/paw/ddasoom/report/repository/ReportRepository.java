package com.paw.ddasoom.report.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.report.domain.Report;
import com.paw.ddasoom.report.domain.ReportStatus;
import com.paw.ddasoom.report.domain.ReportTargetType;

public interface ReportRepository extends JpaRepository<Report, Long> {

  @EntityGraph(attributePaths = {"reporter", "processor"})
  Optional<Report> findByReportIdAndDeletedAtIsNull(Long reportId);
  
  // 중복 신고 선 검증(DB UNIQUE: 최후 방어선)
  boolean existsByReporterAndTargetTypeAndTargetId(Member reporter, ReportTargetType targetType, Long targetId); 

  // 대상 누적 신고 건수 (상세 응답용 — 제재 판단 근거)
  long countByTargetTypeAndTargetIdAndDeletedAtIsNull(ReportTargetType targetType, Long targetId);

  // 목록 — 필터 조합 4종 (qna 방식: null 분기 + 파생 쿼리. 필터 3개째부터 QueryDSL 전환)
  @EntityGraph(attributePaths = "reporter")
  Page<Report> findAllByDeletedAtIsNull(Pageable pageable);

  @EntityGraph(attributePaths = "reporter")
  Page<Report> findAllByStatusAndDeletedAtIsNull(ReportStatus status, Pageable pageable);

  @EntityGraph(attributePaths = "reporter")
  Page<Report> findAllByTargetTypeAndDeletedAtIsNull(ReportTargetType targetType, Pageable pageable);

  @EntityGraph(attributePaths = "reporter")
  Page<Report> findAllByStatusAndTargetTypeAndDeletedAtIsNull(ReportStatus status, ReportTargetType targetType, Pageable pageable);
}