package com.paw.ddasoom.qna.service;

import com.paw.ddasoom.support.controller.AdminFaqController;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.common.dto.PageResponse;
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


  // ====== 1. 유저용 =======

    // 1) 문의 작성
    @Transactional
    public QnaSummaryResponse createQna(Long memberId, QnaCreateRequest request) {
      Member questioner = memberRepository.getReferenceById(memberId);

      Qna qna = Qna.builder()
        .questioner(questioner)
        .title(request.getTitle())
        .content(request.getContent())
        .build();

      Qna savedQna = qnaRepository.save(qna);
      return QnaSummaryResponse.from(savedQna);
    }

    // 2) 문의 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<QnaSummaryResponse> getMyQnas(Long memberId, Pageable pageable) {
        Page<Qna> qnaPage =
                qnaRepository.findByQuestioner_IdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId, pageable);
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
        Qna qna = getQnaEntity(qnaId);
        validateOwner(qna, memberId);

        appendComment(qna, memberRepository.getReferenceById(memberId), request.getContent());
        qna.markPending();

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
    @Transactional (readOnly = true)
    public QnaDetailResponse getAdminQna(Long qnaId) {
      return buildDetail(getQnaEntity(qnaId));
    }

    // 3) 답변 코멘트 추가 → 상태 변경 + 답변일자 업데이트
    @Transactional
    public QnaDetailResponse addAdminComment(Long adminId, Long qnaId, QnaCommentCreateRequest request) {
      Qna qna = getQnaEntity(qnaId);

      appendComment(qna, memberRepository.getReferenceById(adminId), request.getContent());
      qna.markAnswered();

      qnaRepository.flush();
      return buildDetail(qna);
    }

  // ====== 3. 내부 공통 =======
    private Qna getQnaEntity(Long qnaId) {
      return qnaRepository.findByIdAndDeletedAtIsNull(qnaId)
        .orElseThrow(() -> new QnaException(QnaErrorCode.QNA_NOT_FOUND));
    }

    private void validateOwner(Qna qna, Long memberId) {
      if(!qna.getQuestioner().getId().equals(memberId)) {
        throw new QnaException(QnaErrorCode.QNA_ACCESS_DENIED);
      }
    }

    private void appendComment(Qna qna, Member writer, String content) {
        QnaComment comment = QnaComment.builder()
                .qna(qna)
                .member(writer)
                .content(content)
                .build();
        qnaCommentRepository.save(comment);
    }

    private QnaDetailResponse buildDetail(Qna qna) {
      List<QnaComment> comments = 
        qnaCommentRepository.findByQna_IdAndDeletedAtIsNullOrderByCreatedAtAsc(qna.getId());
      return QnaDetailResponse.from(qna, comments);
    }
  }
