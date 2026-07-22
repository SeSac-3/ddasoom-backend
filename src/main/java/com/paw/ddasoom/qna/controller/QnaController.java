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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "1:1 문의(QnA)", description = "사용자 — 내 문의 작성·목록·상세·재질문(코멘트) API")
@SecurityRequirement(name = "bearerAuth")   // 전 경로 로그인 필요 (본인 문의만 접근)
public class QnaController {

  private final QnaService qnaService;

  // 1. 문의 작성
  @Operation(summary = "문의 작성", description = """
          새 1:1 문의를 등록합니다. 상태는 항상 PENDING으로 시작합니다.
          - 인가: USER(로그인 사용자 본인)""")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<QnaSummaryResponse>> createQna(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody QnaCreateRequest request) {
    QnaSummaryResponse response = qnaService.createQna(userDetails.getMemberId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("문의가 등록되었습니다.", response));
  }

  // 2. 내 문의 목록 조회
  @Operation(summary = "내 문의 목록 조회", description = """
          로그인 사용자 본인이 작성한 문의를 페이징으로 조회합니다.
          - 인가: USER(로그인 사용자 본인)""")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
  })
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
  @Operation(summary = "내 문의 상세 조회", description = """
          내 문의 단건을 코멘트 스레드와 함께 조회합니다. 본인 소유가 아니면 403.
          - 인가: USER(본인 소유만)""")
  @Parameter(name = "qnaId", description = "문의 PK", required = true, example = "1")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 문의 아님(QNA_002)"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문의 없음(QNA_001)")
  })
  @GetMapping("/{qnaId}")
  public ResponseEntity<ApiResponse<QnaDetailResponse>> getMyQna(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable("qnaId") Long qnaId) {
    return ResponseEntity.ok(
        ApiResponse.success(qnaService.getMyQna(userDetails.getMemberId(), qnaId)));
  }

  // 4. 내 문의에 코멘트(재질문) 추가
  @Operation(summary = "내 문의 재질문(코멘트) 추가", description = """
          내 문의에 재질문 코멘트를 추가합니다. 추가 시 상태가 다시 PENDING(답변 대기)으로 복귀합니다.
          - 인가: USER(본인 소유만)""")
  @Parameter(name = "qnaId", description = "문의 PK", required = true, example = "1")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 문의 아님(QNA_002)"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문의 없음(QNA_001)")
  })
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