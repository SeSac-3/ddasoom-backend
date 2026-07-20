package com.paw.ddasoom.report.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.board.service.PostCommentService;
import com.paw.ddasoom.board.service.PostService;
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
  private final PostCommentService postCommentService;
  private final EntityManager em;

  @Transactional
  public void createReport(Long memberId, ReportCreateRequest request) {
    if (request.getReason().isContentRequired()
        && (request.getContent() == null
        || request.getContent().isBlank())) {
      throw new ReportException(ReportErrorCode.REPORT_CONTENT_REQUIRED);
    }
    // 본인 신고 방지
    if (request.getTargetType() == ReportTargetType.MEMBER
        && request.getTargetId().equals(memberId)) {
      throw new ReportException(ReportErrorCode.REPORT_SELF);
    }
    
    Member reporter = memberRepository.getReferenceById(memberId);

    if (reportRepository.existsByReporterAndTargetTypeAndTargetId(
        reporter, request.getTargetType(), request.getTargetId())) {
      throw new ReportException(ReportErrorCode.REPORT_DUPLICATE);
        }

      Report report = Report.builder()
        .reporter(reporter)
        .targetType(request.getTargetType())
        .targetId(request.getTargetId())
        .reason(request.getReason())
        .content(request.getContent())
        .build();
      
        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportSummaryResponse> getReports(
        ReportStatus status, ReportTargetType targetType, Pageable pageable) {
      Page<Report> page = findByFilters(status, targetType, pageable);
      return PageResponse.of(page, ReportSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse getReport(Long reportId) {
      Report report = getActiveReport(reportId);
      long targetReportCount = reportRepository
          .countByTargetTypeAndTargetIdAndDeletedAtIsNull(report.getTargetType(), report.getTargetId());
      return ReportDetailResponse.from(report, targetReportCount);
    }

    @Transactional
    public void approveReport(Long adminId, Long reportId) {
      Report report = getActiveReport(reportId);
      Member admin = memberRepository.getReferenceById(adminId);
      report.approve(admin);
      hideTarget(report);   
      em.flush();
    }

    @Transactional
    public void rejectReport(Long adminId, Long reportId) {
      Report report = getActiveReport(reportId);
      Member admin = memberRepository.getReferenceById(adminId);
      report.reject(admin);
      em.flush();
    }

  // ===== private =====
  
    private Report getActiveReport(Long reportId) {
      return reportRepository.findByReportIdAndDeletedAtIsNull(reportId)
        .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));
    }

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

    // private void hideTarget(Report report) {
    //   switch (report.getTargetType()) {
    //     case POST -> postService.forceDeletePost(report.getTargetId());
    //     case POST_COMMENT -> postCommentService.forceDeleteComment(report.getTargetId());
    //     case MEMBER -> hideMember(report.getTargetId());
    //   }
    // }

    // // 이미 탈퇴한 회원
    // private void hideMember(Long targetmemberId) {
    //   Member target = memberRepository.findById(targetmemberId)
    //       .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    //   if (target.isDeleted()) {
    //     return;
    //   }
    //   adminMemberService.forceWithdraw(targetmemberId);
    // }

    /**
     * 승인 = 대상 숨김. 각 대상 도메인의 기존 삭제/탈퇴 경로를 재사용한다.
     * MEMBER는 forceWithdraw 내부에서 RT 삭제 + forceLogout 마커까지 자동 처리됨.
     */
    private void hideTarget(Report report) {
        switch (report.getTargetType()) {
            case POST -> { /* TODO: 창호님 forceDeletePost 머지 후 연결 */ }
            case POST_COMMENT -> { /* TODO: 창호님 forceDeleteComment 머지 후 연결 */ }
            case MEMBER -> hideMember(report.getTargetId());
        }
    }

    /**
     * 이미 탈퇴한 회원이면 no-op — Member.softDelete()가 MEMBER_003을 던지므로
     * 같은 회원 대상 신고 2건 순차 승인 시 두 번째 승인이 터지는 것을 방지.
     */
    private void hideMember(Long targetMemberId) {
        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        if (target.isDeleted()) {
            return;
        }
        adminMemberService.forceWithdraw(targetMemberId);
    }
}
