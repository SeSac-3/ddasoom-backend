package com.paw.ddasoom.qna.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.common.util.PageableSanitizer;
import com.paw.ddasoom.qna.dto.request.QnaCommentCreateRequest;
import com.paw.ddasoom.qna.dto.request.QnaCreateRequest;
import com.paw.ddasoom.qna.dto.response.QnaDetailResponse;
import com.paw.ddasoom.qna.dto.response.QnaSummaryResponse;
import com.paw.ddasoom.qna.service.QnaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * [유저용 문의 API]
 * 관리자용(AdminQnaController)과 컨트롤러만 분리하고 QnaService는 공유 — 도메인 로직 중복 방지
 * 이쪽은 '내 문의'만 다루므로 모든 경로에서 서비스가 소유권(validateOwner)을 검증
 */
@RestController
@RequestMapping("/api/qnas")
@RequiredArgsConstructor
public class QnaController {

  private final QnaService qnaService;

  // 1. 문의 작성
  @PostMapping
  public ResponseEntity<ApiResponse<QnaSummaryResponse>> createQna(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody QnaCreateRequest request) {
    QnaSummaryResponse response = qnaService.createQna(userDetails.getMemberId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("문의가 등록되었습니다.", response));
  }

  // 2. 내 문의 목록 조회
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<QnaSummaryResponse>>> getMyQnas(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PageableDefault(size = 10) Pageable pageable) {
    Pageable safePageable = PageableSanitizer.sanitize(pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"), "createdAt", "answeredAt");
    return ResponseEntity.ok(
        ApiResponse.success(qnaService.getMyQnas(userDetails.getMemberId(), safePageable)));
  }

  // 3. 내 문의 상세 조회
  @GetMapping("/{qnaId}")
  public ResponseEntity<ApiResponse<QnaDetailResponse>> getMyQna(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable("qnaId") Long qnaId) {
    return ResponseEntity.ok(
        ApiResponse.success(qnaService.getMyQna(userDetails.getMemberId(), qnaId)));
  }

  // 4. 내 문의에 코멘트(재질문) 추가
  @PostMapping("/{qnaId}/comments")
  public ResponseEntity<ApiResponse<QnaDetailResponse>> addComment(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable("qnaId") Long qnaId,
      @Valid @RequestBody QnaCommentCreateRequest request) {
    QnaDetailResponse response =
        qnaService.addUserComment(userDetails.getMemberId(), qnaId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("답글이 등록되었습니다.", response));
  }
}