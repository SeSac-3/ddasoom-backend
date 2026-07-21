package com.paw.ddasoom.report.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.board.service.AdminPostService;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.member.service.AdminMemberService;
import com.paw.ddasoom.report.domain.Report;
import com.paw.ddasoom.report.domain.ReportStatus;
import com.paw.ddasoom.report.domain.ReportTargetType;
import com.paw.ddasoom.report.dto.request.ReportCreateRequest;
import com.paw.ddasoom.report.dto.response.ReportDetailResponse;
import com.paw.ddasoom.report.dto.response.ReportSummaryResponse;
import com.paw.ddasoom.report.exception.ReportErrorCode;
import com.paw.ddasoom.report.exception.ReportException;
import com.paw.ddasoom.report.repository.ReportRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final ReportRepository reportRepository;
  private final MemberRepository memberRepository;
  private final AdminMemberService adminMemberService;
  private final AdminPostService adminPostService;
  private final EntityManager em;

  // ====== 1. 유저용 ========

  // 1) 신고 접수
  //    순서: 조건부 필수 검증 → 자기 신고 차단 → 프록시 참조 → 중복 선검증 → 저장
  @Transactional
  public void createReport(Long memberId, ReportCreateRequest request) {
    // 1-1) 조건부 필수 검증 — 'ETC면 상세 사유 필수' 규칙은 enum이 보유(ReportReason.contentRequired)
    if (request.getReason().isContentRequired()
            && (request.getContent() == null
            || request.getContent().isBlank())) {
      throw new ReportException(ReportErrorCode.REPORT_CONTENT_REQUIRED);
    }
    // 1-2) 본인 신고 방지
    if (request.getTargetType() == ReportTargetType.MEMBER
            && request.getTargetId().equals(memberId)) {
      throw new ReportException(ReportErrorCode.REPORT_SELF);
    }

    // 1-3) 인증된 회원은 존재가 보장되므로 SELECT 없이 프록시 참조만 확보 (쓰기 경로 팀 원칙)
    Member reporter = memberRepository.getReferenceById(memberId);

    // 1-4) 중복 신고 선검증 — 친절한 409를 주기 위한 것이고,
    //      동시 요청 경합은 uk_report_reporter_target(DB UNIQUE)이 최종 방어
    if (reportRepository.existsByReporterAndTargetTypeAndTargetId(
            reporter, request.getTargetType(), request.getTargetId())) {
      throw new ReportException(ReportErrorCode.REPORT_DUPLICATE);
    }

    // 1-5) 저장 (status는 빌더 미노출 — 항상 PENDING으로 접수)
    Report report = Report.builder()
            .reporter(reporter)
            .targetType(request.getTargetType())
            .targetId(request.getTargetId())
            .reason(request.getReason())
            .content(request.getContent())
            .build();

    reportRepository.save(report);
  }

  // ====== 2. 관리자용 ========

  // 1) 신고 큐 목록 조회 (status/targetType 필터 optional)
  @Transactional(readOnly = true)
  public PageResponse<ReportSummaryResponse> getReports(
          ReportStatus status, ReportTargetType targetType, Pageable pageable) {
    Page<Report> page = findByFilters(status, targetType, pageable);
    return PageResponse.of(page, ReportSummaryResponse::from);
  }

  // 2) 신고 상세 조회 (+ 대상 누적 신고 건수 — 제재 판단 근거)
  @Transactional(readOnly = true)
  public ReportDetailResponse getReport(Long reportId) {
    Report report = getActiveReport(reportId);
    long targetReportCount = reportRepository
            .countByTargetTypeAndTargetIdAndDeletedAtIsNull(report.getTargetType(), report.getTargetId());
    return ReportDetailResponse.from(report, targetReportCount);
  }

  // 3) 신고 승인 — '판정'과 '실행'을 분리
  //    판정(상태 전이 + 처리자 기록)은 엔티티가, 실행(대상 숨김)은 hideTarget이 담당
  @Transactional
  public void approveReport(Long adminId, Long reportId) {
    Report report = getActiveReport(reportId);
    Member admin = memberRepository.getReferenceById(adminId);
    report.approve(admin);
    hideTarget(report);
    // UNIQUE/제약 위반을 커밋 시점이 아니라 이 메서드 안에서 조기에 터뜨리기 위한 flush
    em.flush();
  }

  // 4) 신고 반려 — 판정만 수행 (대상에 대한 실행 없음)
  @Transactional
  public void rejectReport(Long adminId, Long reportId) {
    Report report = getActiveReport(reportId);
    Member admin = memberRepository.getReferenceById(adminId);
    report.reject(admin);
    // 승인과 동일하게, 제약 위반을 이 메서드 경계 안에서 조기에 드러냄
    em.flush();
  }

  // ===== private =====

  // 1) 활성 신고 단건 조회 공통 (논리삭제X 데이터만)
  private Report getActiveReport(Long reportId) {
    return reportRepository.findByReportIdAndDeletedAtIsNull(reportId)
            .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));
  }

  // 2) 필터 조합 4종 분기 (null 분기 + 파생 쿼리. 필터가 3개째로 늘면 QueryDSL 전환)
  private Page<Report> findByFilters(ReportStatus status, ReportTargetType targetType, Pageable pageable) {
    if (status != null && targetType != null) {
      return reportRepository.findAllByStatusAndTargetTypeAndDeletedAtIsNull(status, targetType, pageable);
    }
    if (status != null) {
      return reportRepository.findAllByStatusAndDeletedAtIsNull(status, pageable);
    }
    if (targetType != null) {
      return reportRepository.findAllByTargetTypeAndDeletedAtIsNull(targetType, pageable);
    }
    return reportRepository.findAllByDeletedAtIsNull(pageable);
  }

  /**
   * 승인 = 대상 숨김. 각 대상 도메인의 기존 삭제/탈퇴 경로를 재사용한다.
   * MEMBER는 forceWithdraw 내부에서 RT 삭제 + forceLogout 마커까지 자동 처리됨.
   */
  // 3) 제재 전용 시스템을 새로 만들지 않고 기존 삭제/탈퇴 경로에 위임 —
  //    회원 상태의 '진실'을 각 도메인 한 곳에만 두기 위한 선택
  private void hideTarget(Report report) {
    switch (report.getTargetType()) {
      // 승인 = 기존 관리자 강제삭제 경로 재사용 (제재 로직을 board 도메인 한 곳에만 둠).
      // 두 메서드 모두 멱등 — 같은 대상 신고 2건이 순차 승인돼도 두 번째는 no-op.
      case POST -> adminPostService.forceDeletePost(report.getTargetId());
      case POST_COMMENT -> adminPostService.forceDeleteComment(report.getTargetId());
      case MEMBER -> adminMemberService.hideMember(report.getTargetId());
    }
  }

  /**
   * 이미 탈퇴한 회원이면 no-op — Member.softDelete()가 MEMBER_003을 던지므로
   * 같은 회원 대상 신고 2건 순차 승인 시 두 번째 승인이 터지는 것을 방지.
   */
  // 4) 회원 숨김 = 기존 강제 탈퇴 경로(adminMemberService.forceWithdraw) 재사용
  private void hideMember(Long targetMemberId) {
    Member target = memberRepository.findById(targetMemberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    if (target.isDeleted()) {
      return;
    }
    adminMemberService.forceWithdraw(targetMemberId);
  }
}