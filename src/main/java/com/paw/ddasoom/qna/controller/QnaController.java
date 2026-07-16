package com.paw.ddasoom.qna.controller;

import org.springframework.data.domain.Pageable;
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
import com.paw.ddasoom.qna.dto.request.QnaCommentCreateRequest;
import com.paw.ddasoom.qna.dto.request.QnaCreateRequest;
import com.paw.ddasoom.qna.dto.response.QnaDetailResponse;
import com.paw.ddasoom.qna.dto.response.QnaSummaryResponse;
import com.paw.ddasoom.qna.service.QnaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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
    return ResponseEntity.ok(
        ApiResponse.success(qnaService.getMyQnas(userDetails.getMemberId(), pageable)));
  }

  // 3. 내 문의 상세 조회
  @GetMapping("/{qnaId}")
  public ResponseEntity<ApiResponse<QnaDetailResponse>> getMyQna(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long qnaId) {
    return ResponseEntity.ok(
        ApiResponse.success(qnaService.getMyQna(userDetails.getMemberId(), qnaId)));
  }

  // 4. 내 문의에 코멘트(재질문) 추가
  @PostMapping("/{qnaId}/comments")
  public ResponseEntity<ApiResponse<QnaDetailResponse>> addComment(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long qnaId,
      @Valid @RequestBody QnaCommentCreateRequest request) {
    QnaDetailResponse response =
        qnaService.addUserComment(userDetails.getMemberId(), qnaId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("문의가 등록되었습니다.", response));
  }
}