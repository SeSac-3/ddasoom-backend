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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.qna.domain.QnaStatus;
import com.paw.ddasoom.qna.dto.request.QnaCommentCreateRequest;
import com.paw.ddasoom.qna.dto.response.QnaDetailResponse;
import com.paw.ddasoom.qna.dto.response.QnaSummaryResponse;
import com.paw.ddasoom.qna.service.QnaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * [관리자용 문의 API]
 * 유저용(QnaController)과 컨트롤러만 분리하고 QnaService는 공유
 * /api/admin/** 는 SecurityConfig가 URL 레벨에서 자동 잠그므로 컨트롤러에 권한 체크 코드가 없음
 */
@RestController
@RequestMapping("/api/admin/qnas")
@RequiredArgsConstructor
public class AdminQnaController {

    private final QnaService qnaService;

    // 1. 전체 문의 목록 조회 (상태 필터 optional: PENDING / ANSWERED / 미지정=전체)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QnaSummaryResponse>>> getAdminQnas(
            @RequestParam(name = "status", required = false) QnaStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.getAdminQnas(status, pageable)));
    }

    // 2. 문의 상세 조회
    @GetMapping("/{qnaId}")
    public ResponseEntity<ApiResponse<QnaDetailResponse>> getAdminQna(@PathVariable("qnaId") Long qnaId) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.getAdminQna(qnaId)));
    }

    // 3. 답변 코멘트 추가 (→ 상태 ANSWERED)
    @PostMapping("/{qnaId}/comments")
    public ResponseEntity<ApiResponse<QnaDetailResponse>> addComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("qnaId") Long qnaId,
            @Valid @RequestBody QnaCommentCreateRequest request) {
        QnaDetailResponse response =
                qnaService.addAdminComment(userDetails.getMemberId(), qnaId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("답변이 등록되었습니다.", response));
    }
}