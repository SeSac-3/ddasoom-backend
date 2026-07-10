package com.paw.ddasoom.support.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.support.domain.Notice;
import com.paw.ddasoom.support.dto.request.NoticeCreateRequest;
import com.paw.ddasoom.support.dto.request.NoticeUpdateRequest;
import com.paw.ddasoom.support.dto.response.NoticeResponse;
import com.paw.ddasoom.support.dto.response.NoticeSummaryResponse;
import com.paw.ddasoom.support.exception.SupportErrorCode;
import com.paw.ddasoom.support.exception.SupportException;
import com.paw.ddasoom.support.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;


// ================================
// 공지사항 로직 서비스
// ================================
@Service
@RequiredArgsConstructor
public class NoticeService {

  private final NoticeRepository noticeRepository;
  private final MemberRepository memberRepository;

  // ====== 1. 유저용 ========

  // 1) 공지사항 전체 목록 조회
  @Transactional(readOnly = true)
  public PageResponse<NoticeSummaryResponse> getNotices(Pageable pageable) {
    Page<Notice> noticePage = noticeRepository.findAllForUser(pageable);
    return PageResponse.of(noticePage, NoticeSummaryResponse::from);
  }

  // 2) 공지사항 상세 조회
  @Transactional(readOnly = true)
  public NoticeResponse getNotice(Long noticeId) {
      Notice notice = getNoticeEntity(noticeId);

  // 비노출 공지 = 유저기준 "삭제"와 동일 취급
  if (!notice.getIsVisible()) {
    throw new SupportException(SupportErrorCode.NOTICE_NOT_FOUND);
  }
  return NoticeResponse.from(notice);
}
  // ====== 2. 관리자용 ========

  // 1) 공지사항 전체 목록 조회 (비노출, 삭제 포함)
  @Transactional(readOnly = true)
  public PageResponse<NoticeSummaryResponse> getAdminNotices(Pageable pageable) {
    Page<Notice> noticePage = noticeRepository.findAllForAdmin(pageable);
    return PageResponse.of(noticePage, NoticeSummaryResponse::from);
  }

  // 2) 공지사항 상제 조회
  @Transactional(readOnly = true)
  public NoticeResponse getAdminNotice(Long noticeId) {
      return NoticeResponse.from(getNoticeEntity(noticeId));
  }
  
  // 3) 새로운 공지사항 등록
  @Transactional
  public NoticeResponse createNotice(Long memberId, NoticeCreateRequest request) {
    Member member = memberRepository.findById(memberId)
      .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

    // 3-1) 요청 데이터
    Notice notice = Notice.builder()
      .member(member)
      .title(request.getTitle())
      .content(request.getContent())
      .build();

    // 3-2) DB 저장 이후 Response DTO 변환 후 반환
    return NoticeResponse.from(noticeRepository.save(notice));
  }
  
  // 4) 공지사항 수정
  @Transactional
  public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
    Notice notice = getNoticeEntity(noticeId);
    notice.update(request.getTitle(), request.getContent());
    return NoticeResponse.from(notice);
  }

  // 5) 공지사항 노출여부 변경
  @Transactional
  public void changeVisibility(Long noticeId, boolean isVisible) {
    Notice notice = getNoticeEntity(noticeId);
    notice.changeVisibility(isVisible);
  }

  // 6) 공지사항 삭제
  @Transactional
  public void deleteNotice(Long noticeId) {
      Notice notice = getNoticeEntity(noticeId);
      notice.softDelete();
  }

  /** 
   * 7) 고정된 공지사항 순서 재정렬
   * @param orderedNoticeIds 사용자가 지정한 '상단 고정글 ID 리스트' 
   */
  @Transactional
  public void reorderPinned(List<Long> orderedNoticeIds) {
    List<Notice> currentlyPinned = noticeRepository.findAllPinned();

    // 7-1) 기존 고정글 해제 (빈 리스트 전송 시 = 전체 해제)
    currentlyPinned.stream()
            .filter(notice -> !orderedNoticeIds.contains(notice.getId()))
            .forEach(Notice::unpin);

    // 7-2) 고정글 순서 부여
    for (int i = 0; i < orderedNoticeIds.size(); i++) {
        Notice notice = getNoticeEntity(orderedNoticeIds.get(i));
        notice.pin(i + 1);
    }
  }

// ====== 3. 내부 조회 ========

  // 1) 공지사항 단건 조회 공통 내부 메서드 (논리삭제X 데이터만 조회)
  private Notice getNoticeEntity(Long noticeId) {
  return noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
    .orElseThrow(() -> new SupportException(SupportErrorCode.NOTICE_NOT_FOUND));
  }
}
