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

  // 상세 조회 — 응답에 신고자·처리자·피신고자 닉네임이 모두 필요하므로 한 번에 fetch (N+1 차단)
  @EntityGraph(attributePaths = {"reporter", "processor", "reportedMember"})
  Optional<Report> findByReportIdAndDeletedAtIsNull(Long reportId);
  
  // 중복 신고 선 검증(DB UNIQUE: 최후 방어선)
  boolean existsByReporterAndTargetTypeAndTargetId(Member reporter, ReportTargetType targetType, Long targetId); 

  // 대상 누적 신고 건수 (상세 응답용 — 제재 판단 근거)
  // 반려(REJECTED)를 StatusNot으로 집계에서 제외 / 승인 = 강제탈퇴라 허위 신고가 섞이면 오판 비용이 계정 삭제다.
  long countByTargetTypeAndTargetIdAndStatusNotAndDeletedAtIsNull(
          ReportTargetType targetType, Long targetId, ReportStatus excludedStatus);

  // 목록 — 필터 조합 4종 (qna 방식: null 분기 + 파생 쿼리. 필터 3개째부터 QueryDSL 전환)
  // 목록에 피신고자 닉네임을 표시하므로 미적용 시 N+1.
  @EntityGraph(attributePaths = {"reporter", "reportedMember"})
  Page<Report> findAllByDeletedAtIsNull(Pageable pageable);

  @EntityGraph(attributePaths = {"reporter", "reportedMember"})
  Page<Report> findAllByStatusAndDeletedAtIsNull(ReportStatus status, Pageable pageable);

  @EntityGraph(attributePaths = {"reporter", "reportedMember"})
  Page<Report> findAllByTargetTypeAndDeletedAtIsNull(ReportTargetType targetType, Pageable pageable);

  @EntityGraph(attributePaths = {"reporter", "reportedMember"})
  Page<Report> findAllByStatusAndTargetTypeAndDeletedAtIsNull(ReportStatus status, ReportTargetType targetType, Pageable pageable);

  // 피신고자가 받은 누적 신고 건수 (상세 응답 — 상습성 판단 근거). 대상 카운트와 동일 정책으로 반려 제외.
  // 언더스코어(reportedMember_Id): 언더스코어 없이 쓰면 훗날 Report에 reportedMemberId 필드가 생겼을 때 조용히 다른 걸 가리킨다.
  long countByReportedMember_IdAndStatusNotAndDeletedAtIsNull(Long memberId, ReportStatus excludedStatus);

  // 관리자 회원 상세
  // status/targetType과 조합하지 않고 단독 필터로 둔 이유: 조합하면 파생 쿼리가 4종 → 8종으로 늘어난다.
  // 이 필터는 회원 상세 화면 전용(단일 진입점)이라 조합 수요가 없어 조기 분기로 끝낸다.
  // 필터가 실제로 조합돼야 하는 시점이 오면 그때 QueryDSL로 전환 (팀 원칙: 동적 필터 2개 이상)
  // 추가 — count(위)는 반려 제외, 목록(아래)은 반려 포함: 의도된 차이. 목록은 '판정 이력 열람'이 목적이라 반려 내역도 보여야 한다.

  @EntityGraph(attributePaths = {"reporter", "reportedMember"})
  Page<Report> findAllByReportedMember_IdAndDeletedAtIsNull(Long memberId, Pageable pageable);
}