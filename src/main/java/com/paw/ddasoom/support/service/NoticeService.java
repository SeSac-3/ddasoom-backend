package com.paw.ddasoom.support.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.service.ImageService;
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
  private final ImageService imageService;

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

  // 2-1) 비노출 공지 = 유저기준 "삭제"와 동일 취급
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

  // 2) 공지사항 상세 조회
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

  // 3-2) DB 저장 (이미지 연결에 필요한 noticeId 확보)
  Notice savedNotice = noticeRepository.save(notice);

  // 3-3) 본문 이미지 확정 연결 (리스트 순서 = image_order)
  imageService.attach(request.getImageIds(), OwnerType.NOTICE, savedNotice.getId());
  return NoticeResponse.from(savedNotice);
  }
  
  // 4) 공지사항 수정
  @Transactional
  public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
    Notice notice = getNoticeEntity(noticeId);
    notice.update(request.getTitle(), request.getContent());

    //4-1) 최종 imageIds 전체로 diff (제외 이미지: soft delete + 순서 갱신)
    imageService.syncImages(request.getImageIds(),OwnerType.NOTICE, noticeId);
    noticeRepository.flush();
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

      // 6-1) 소유자 삭제 시 이미지 정리
      imageService.syncImages(List.of(),OwnerType.NOTICE, noticeId);
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
    Map<Long, Notice> noticeMap = noticeRepository
      .findAllByIdInAndDeletedAtIsNull(orderedNoticeIds).stream()
      .collect(Collectors.toMap(Notice::getId, notice -> notice));
    
      for (int i=0; i<orderedNoticeIds.size(); i++) {
        Notice notice = noticeMap.get(orderedNoticeIds.get(i));
        if (notice == null) {
          throw new SupportException(SupportErrorCode.NOTICE_NOT_FOUND);
        }
        notice.pin(i+1);
      }
    
  }

// ====== 3. 내부 조회 ========

  // 1) 공지사항 단건 조회 공통 내부 메서드 (논리삭제X 데이터만 조회)
  private Notice getNoticeEntity(Long noticeId) {
  return noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
    .orElseThrow(() -> new SupportException(SupportErrorCode.NOTICE_NOT_FOUND));
  }
}
