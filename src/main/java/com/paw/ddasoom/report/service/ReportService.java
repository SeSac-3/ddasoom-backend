package com.paw.ddasoom.report.service;

import org.jsoup.Jsoup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.domain.PostComment;
import com.paw.ddasoom.board.repository.PostCommentRepository;
import com.paw.ddasoom.board.repository.PostRepository;
import com.paw.ddasoom.board.service.AdminPostService;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.member.domain.Member;
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

  // 읽기전용
  private final PostRepository postRepository;
  private final PostCommentRepository postCommentRepository;

  private static final int SNIPPET_LENGTH = 200;
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

    // 1-3) 신고 대상 실존 검증
    //      기존에는 existsById로 '있는지'만 확인했으나, 이제 같은 조회에서 스냅샷(소유자·제목·발췌)까지 함께 확보
    //      중복 검증(1-5)보다 앞에 두는 이유: 없는 대상은 중복 여부를 따질 이유 자체가 없음.
    TargetSnapshot snapshot = resolveTarget(request.getTargetType(), request.getTargetId());

    // 1-4) 인증된 회원은 존재가 보장되므로 SELECT 없이 프록시 참조만 확보 (쓰기 경로 팀 원칙)
    Member reporter = memberRepository.getReferenceById(memberId);

    // 1-5) 중복 신고 선검증 — 친절한 409를 주기 위한 것이고,
    //      동시 요청 경합은 uk_report_reporter_target(DB UNIQUE)이 최종 방어
    if (reportRepository.existsByReporterAndTargetTypeAndTargetId(
            reporter, request.getTargetType(), request.getTargetId())) {
      throw new ReportException(ReportErrorCode.REPORT_DUPLICATE);
    }

    // 1-6) 저장 (status는 빌더 미노출 — 항상 PENDING으로 접수)
    Report report = Report.builder()
            .reporter(reporter)
            .targetType(request.getTargetType())
            .targetId(request.getTargetId())
            .reason(request.getReason())
            .content(request.getContent())
            // 접수 시점 대상 스냅샷 고정
            .reportedMember(snapshot.owner())
            .targetParentId(snapshot.parentId())
            .targetTitle(snapshot.title())
            .targetSnippet(snapshot.snippet())
            .build();

    reportRepository.save(report);
  }

  // ====== 2. 관리자용 ========

  // 1) 신고 큐 목록 조회 (status/targetType 필터 optional)
  //    reportedMemberId 파라미터: 지정 시 해당 회원이 받은 신고만 조회(관리자 회원 상세 전용, 다른 필터 무시)
  @Transactional(readOnly = true)
  public PageResponse<ReportSummaryResponse> getReports(
          ReportStatus status, ReportTargetType targetType, Long reportedMemberId, Pageable pageable) {
    Page<Report> page = findByFilters(status, targetType, reportedMemberId, pageable);
    return PageResponse.of(page, ReportSummaryResponse::from);
  }

  // 2) 신고 상세 조회 (+ 대상 누적 신고 건수 — 제재 판단 근거)
  @Transactional(readOnly = true)
  public ReportDetailResponse getReport(Long reportId) {
    Report report = getActiveReport(reportId);

    // 대상 기준 누적, 반려(허위 판정) 제외 (StatusNot에 REJECTED 전달)
    long targetReportCount = reportRepository
            .countByTargetTypeAndTargetIdAndStatusNotAndDeletedAtIsNull(
                    report.getTargetType(), report.getTargetId(), ReportStatus.REJECTED);

    // 회원 기준 누적. 동일 정책(반려 제외). 스냅샷 이전 데이터는 피신고자가 없어 0
    long reportedMemberReportCount = report.getReportedMember() == null
            ? 0L
            : reportRepository.countByReportedMember_IdAndStatusNotAndDeletedAtIsNull(
                    report.getReportedMember().getId(), ReportStatus.REJECTED);

    return ReportDetailResponse.from(report, targetReportCount, reportedMemberReportCount);
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

  // 2) 필터 조합 분기 (null 분기 + 파생 쿼리. 필터가 늘면 QueryDSL 전환)
  private Page<Report> findByFilters(
          ReportStatus status, ReportTargetType targetType, Long reportedMemberId, Pageable pageable) {

    //  회원 필터 조기 분기 (기존 4종 분기보다 앞).
    // status/targetType과 조합하지 않는다. 조합을 허용하면 파생 쿼리가 4종 → 8종으로 늘어나는데,
    // 이 필터는 관리자 회원 상세 화면 전용(단일 진입점)이라 조합 수요 자체가 없다.
    // 조합이 실제로 필요해지는 시점이 곧 QueryDSL 전환 시점이다(팀 원칙: 동적 필터 2개 이상).

    if (reportedMemberId != null) {
      return reportRepository.findAllByReportedMember_IdAndDeletedAtIsNull(reportedMemberId, pageable);
    }

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
   * 신고 대상 해석 — 실존 검증 + 스냅샷 확보 (기존 validateTargetExists 대체)
   *
   * <p>Report는 (targetType, targetId) 논리 참조라 FK가 없다. 접수 시 막지 않으면 존재하지 않는
   * targetId가 그대로 저장되고, 승인 시점 hideTarget이 각 도메인의 NOT_FOUND를 던져
   * <b>판정 트랜잭션 전체가 롤백</b>된다. 이 검증 책임은 그대로 유지한다.
   *
   * <p>기존 existsById 대신 엔티티를 실제로 읽는 이유: 어차피 스냅샷 때문에 대상을 조회해야 하므로,
   * 검증만 따로 하면 같은 row를 두 번 읽게 된다. 쿼리 수는 그대로이고 얻는 정보만 늘었다.
   *
   * <p>삭제 여부를 따지지 않는 이유는 기존과 동일 — 이미 삭제된 게시글/댓글도 신고 접수는 가능해야 하고,
   * 그 경우 승인 시 forceDelete가 멱등 no-op으로 처리한다.
   * 본 프로젝트는 @SQLRestriction을 쓰지 않으므로 findById가 곧 'row 존재'를 의미한다
   * — soft delete 자동 필터가 도입되면 이 메서드가 먼저 깨진다.
   */
  private TargetSnapshot resolveTarget(ReportTargetType targetType, Long targetId) {
    // default 없는 exhaustive switch — ReportTargetType에 값이 추가되면 컴파일 에러로 누락을 강제 탐지.
    // (OwnerType.isPublic()의 boolean 분기와 반대 판단: 여기는 누락 = 검증 우회라 안전한 기본값이 없음)
    return switch (targetType) {
      case POST -> {
        Post post = postRepository.findById(targetId)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_TARGET_NOT_FOUND));
        // post.getMember()는 LAZY 프록시 — 스냅샷 FK로만 쓰이므로 여기서 초기화되지 않는다(추가 SELECT 없음)
        yield new TargetSnapshot(post.getMember(), null, post.getTitle(), toSnippet(post.getContent()));
      }
      case POST_COMMENT -> {
        PostComment comment = postCommentRepository.findById(targetId)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_TARGET_NOT_FOUND));
        // getPost().getId()는 프록시로 해결되지만 getTitle()은 초기화가 필요 — SELECT 1회 추가.
        // 접수는 저빈도 쓰기 경로라 허용한다(조회 경로였다면 @EntityGraph로 묶었을 것).
        // 원글 제목을 스냅샷에 넣는 이유: 댓글만으로는 관리자가 어떤 맥락의 댓글인지 알 수 없다.
        yield new TargetSnapshot(
                comment.getMember(),
                comment.getPost().getId(),
                comment.getPost().getTitle(),
                toSnippet(comment.getContent()));
      }
      case MEMBER -> {
        Member target = memberRepository.findById(targetId)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_TARGET_NOT_FOUND));
        // 회원 신고는 대상 본인이 곧 피신고자. 본문이 없으므로 snippet은 null,
        // 제목 자리에는 닉네임을 넣어 관리자 화면이 targetType 분기 없이 동일하게 렌더할 수 있게 한다.
        yield new TargetSnapshot(target, null, target.getNickname(), null);
      }
    };
  }

  /**
   * 대상 해석 결과.
   * 단일 사용처(resolveTarget → createReport)라 별도 DTO 파일을 만들지 않고 내부 record로 둔다.
   */
  private record TargetSnapshot(Member owner, Long parentId, String title, String snippet) {}

  /**
   * 본문 HTML → 태그·엔티티 제거한 평문의 앞 SNIPPET_LENGTH자.
   *
   * <p>PostResponse.toPreview와 동일한 Jsoup 방식이다. 태그 제거를 '먼저' 하고 자르는 순서도 같다
   * — 본문 앞에 이미지가 오는 경우 태그를 안 지우고 자르면 발췌가 통째로 img 태그가 된다.
   *
   * <p>DB 컬럼이 VARCHAR(500)이고 여기서 200자로 자르므로, 멀티바이트를 감안해도 저장 실패는 나지 않는다.
   */
  private String toSnippet(String html) {
    if (html == null || html.isBlank()) {
      return null;
    }
    String text = Jsoup.parse(html).text();   // 태그 제거 + &nbsp; 등 엔티티 디코딩
    return text.length() > SNIPPET_LENGTH ? text.substring(0, SNIPPET_LENGTH) : text;
  }

  /**
   * 승인 실행 — 대상 콘텐츠 제재 + 피신고자 강제탈퇴.
   *
   * <p>제재 전용 시스템을 새로 만들지 않고 각 도메인의 기존 삭제/탈퇴 경로에 위임한다.
   * 회원·게시글 상태의 '진실'을 각 도메인 한 곳에만 두기 위한 선택이다.
   *
   * <p>모든 경로가 멱등 — 같은 대상 신고 2건이 순차 승인돼도 두 번째는 no-op이다.
   */
  private void hideTarget(Report report) {
    // 1) 콘텐츠 제재 — 기존 관리자 강제삭제 경로 재사용(제재 로직을 board 도메인 한 곳에만 둠).
    switch (report.getTargetType()) {
      case POST -> adminPostService.forceDeletePost(report.getTargetId());
      case POST_COMMENT -> adminPostService.forceDeleteComment(report.getTargetId());
      case MEMBER -> { }
    }

    // 2) 피신고자 강제탈퇴 — 승인 = 계정 제재까지 (팀 결정).
    /*
     * targetType과 무관하게 스냅샷의 reportedMember 하나만 보면 되므로 분기가 필요 없다
     * (MEMBER 타입은 대상 본인이 곧 피신고자라 자연히 커버된다).
     *
     * null 가드가 필요한 이유: 스냅샷 도입(V14) 이전에 접수된 신고는 reportedMember가 NULL이다.
     * 그 경우 콘텐츠 제재만 수행하고 계정은 건드리지 않는다.
     *
     * forceWithdraw가 아니라 withdrawByReport를 쓰는 이유는 멱등성이다. forceWithdraw는 활성 조회를
     * 타므로 이미 탈퇴한 회원이면 MEMBER_003을 던지고, 그러면 승인 트랜잭션 전체가 롤백된다.
     * 한 회원이 여러 건 신고당해 순차 승인되는 건 정상 시나리오라 예외로 막으면 안 된다.
     * (ADMIN 대상도 withdrawByReport 내부에서 no-op — 예외를 던지면 콘텐츠 삭제까지 취소된다)
     *
     * getId()는 LAZY 프록시에서 추가 SELECT 없이 해결된다.
     */
    if (report.getReportedMember() != null) {
      adminMemberService.withdrawByReport(report.getReportedMember().getId());
    }
  }

}