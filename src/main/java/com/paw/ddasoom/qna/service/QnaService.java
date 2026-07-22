package com.paw.ddasoom.qna.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.image.service.ImageService;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.qna.domain.Qna;
import com.paw.ddasoom.qna.domain.QnaComment;
import com.paw.ddasoom.qna.domain.QnaStatus;
import com.paw.ddasoom.qna.dto.request.QnaCommentCreateRequest;
import com.paw.ddasoom.qna.dto.request.QnaCreateRequest;
import com.paw.ddasoom.qna.dto.response.QnaDetailResponse;
import com.paw.ddasoom.qna.dto.response.QnaSummaryResponse;
import com.paw.ddasoom.qna.exception.QnaErrorCode;
import com.paw.ddasoom.qna.exception.QnaException;
import com.paw.ddasoom.qna.repository.QnaCommentRepository;
import com.paw.ddasoom.qna.repository.QnaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QnaService {

  private final QnaRepository qnaRepository;
  private final QnaCommentRepository qnaCommentRepository;
  private final MemberRepository memberRepository;
  private final ImageService imageService;

  // ====== 1. 유저용 =======

  // 1) 문의 작성
  @Transactional
  public QnaSummaryResponse createQna(Long memberId, QnaCreateRequest request) {
    Member questioner = memberRepository.getReferenceById(memberId);

    // 1-1) 요청 데이터 (status는 빌더 미노출 — 항상 PENDING으로 시작)
    Qna qna = Qna.builder()
            .questioner(questioner)
            .title(request.getTitle())
            .content(request.getContent())
            .build();

    // 1-2) DB 저장 (이미지 연결에 필요한 qnaId 확보)
    Qna savedQna = qnaRepository.save(qna);

    // 1-3) 본문 이미지 확정 연결 (리스트 순서 = image_order)
    imageService.attach(request.getImageIds(), OwnerType.QNA, savedQna.getId(), memberId);
    return QnaSummaryResponse.from(savedQna);
  }

  // 2) 문의 목록 조회
  @Transactional(readOnly = true)
  public PageResponse<QnaSummaryResponse> getMyQnas(Long memberId, Pageable pageable) {
    Page<Qna> qnaPage = qnaRepository.findByQuestioner_IdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId, pageable);
    return PageResponse.of(qnaPage, QnaSummaryResponse::from);
  }

  // 3) 문의 상세 조회 (코멘트 스레드 포함 + 소유권 검증)
  @Transactional(readOnly = true)
  public QnaDetailResponse getMyQna(Long memberId, Long qnaId) {
    Qna qna = getQnaEntity(qnaId);
    validateOwner(qna, memberId);
    return buildDetail(qna);
  }

  // 4) 문의 추가 질문(코멘트) → 상태 PENDING 변경
  @Transactional
  public QnaDetailResponse addUserComment(Long memberId, Long qnaId, QnaCommentCreateRequest request) {
    // 4-1) 조회 + 데이터 소유권 검증
    Qna qna = getQnaEntity(qnaId);
    validateOwner(qna, memberId);

    // 4-2) 코멘트 적재 + 첨부 이미지 연결
    QnaComment comment = appendComment(qna, memberRepository.getReferenceById(memberId), request.getContent());
    imageService.attach(request.getImageIds(), OwnerType.QNA_COMMENT, comment.getId(), memberId);

    // 4-3) 재질문이므로 답변 대기 상태로 복귀 (answeredAt은 유지)
    qna.markPending();

    // 4-4) created_at/updated_at은 DB가 계산(@Generated)하므로, 응답 조립 전 DB가 찍은 시각을 엔티티에
    // 동기화
    qnaRepository.flush();
    return buildDetail(qna);
  }

  // ====== 2. 관리자용 =======

  // 1) 전체 문의 목록
  @Transactional(readOnly = true)
  public PageResponse<QnaSummaryResponse> getAdminQnas(QnaStatus status, Pageable pageable) {
    Page<Qna> qnaPage = (status == null)
            ? qnaRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageable)
            : qnaRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status, pageable);
    return PageResponse.of(qnaPage, QnaSummaryResponse::from);
  }

  // 2) 문의 상세 조회
  @Transactional(readOnly = true)
  public QnaDetailResponse getAdminQna(Long qnaId) {
    return buildDetail(getQnaEntity(qnaId));
  }

  // 3) 답변 코멘트 추가 → 상태 변경 + 답변일자 업데이트
  @Transactional
  public QnaDetailResponse addAdminComment(Long adminId, Long qnaId, QnaCommentCreateRequest request) {
    // 3-1) 관리자 경로는 소유권 검증 X (URL 레벨에서 권한 확인)
    Qna qna = getQnaEntity(qnaId);

    // 3-2) 답변 코멘트 적재 + 첨부 이미지 연결
    QnaComment comment = appendComment(qna, memberRepository.getReferenceById(adminId), request.getContent());
    imageService.attach(request.getImageIds(), OwnerType.QNA_COMMENT, comment.getId(), adminId);
    qna.markAnswered();
    qnaRepository.flush();
    return buildDetail(qna);
  }

  // ====== 3. 내부 공통 =======

  // 1) 문의 단건 조회 공통 내부 메서드 (논리삭제X 데이터만 조회)
  private Qna getQnaEntity(Long qnaId) {
    return qnaRepository.findByIdAndDeletedAtIsNull(qnaId)
            .orElseThrow(() -> new QnaException(QnaErrorCode.QNA_NOT_FOUND));
  }

  // 2) 데이터 소유권 검증
  private void validateOwner(Qna qna, Long memberId) {
    if (!qna.getQuestioner().getId().equals(memberId)) {
      throw new QnaException(QnaErrorCode.QNA_ACCESS_DENIED);
    }
  }

  // 3) 코멘트 생성 공통 — 유저/관리자 경로가 같은 적재 로직을 공유
  private QnaComment appendComment(Qna qna, Member writer, String content) {
    QnaComment comment = QnaComment.builder()
            .qna(qna)
            .member(writer)
            .content(content)
            .build();
    return qnaCommentRepository.save(comment);
  }

  // 4) 상세 응답 조립 — 스레드 + 질문 이미지 + 코멘트별 이미지
  private QnaDetailResponse buildDetail(Qna qna) {
    // 4-1) 활성 코멘트 스레드 (작성순)
    List<QnaComment> comments = qnaCommentRepository.findByQna_IdAndDeletedAtIsNullOrderByCreatedAtAsc(qna.getId());

    // 4-2) QnA 첨부는 비공개 버킷 + 30분 만료 Presigned URL — URL은 저장값이 아니라 응답 시점에 발급됨
    List<ImageResponse> questionImages = imageService.getImages(OwnerType.QNA, qna.getId());
    List<Long> commentIds = comments.stream().map(QnaComment::getId).toList();

    // 4-3) commentIds를 IN 절로 한 번에 조회 후 Map<코멘트ID, 이미지목록>으로 그룹핑 (쿼리 1회)
    Map<Long, List<ImageResponse>> commentImagesByOwner = imageService.getImagesGroupedByOwners(OwnerType.QNA_COMMENT,
            commentIds);
    return QnaDetailResponse.from(qna, comments, questionImages, commentImagesByOwner);
  }
}