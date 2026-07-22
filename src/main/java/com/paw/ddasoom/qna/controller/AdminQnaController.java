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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.common.util.PageableSanitizer;
import com.paw.ddasoom.qna.domain.QnaStatus;
import com.paw.ddasoom.qna.dto.request.QnaCommentCreateRequest;
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
 * [관리자용 문의 API]
 * 유저용(QnaController)과 컨트롤러만 분리하고 QnaService는 공유
 * /api/admin/** 는 SecurityConfig가 URL 레벨에서 자동 잠그므로 컨트롤러에 권한 체크 코드가 없음
 */
@RestController
@RequestMapping("/api/admin/qnas")
@RequiredArgsConstructor
@Tag(name = "관리자 문의(Admin QnA)", description = "관리자 — 전체 문의 목록·상세·답변(코멘트) API")
@SecurityRequirement(name = "bearerAuth")   // /api/admin 하위 = ADMIN 전용
public class AdminQnaController {

    private final QnaService qnaService;

    // 1. 전체 문의 목록 조회 (상태 필터 optional: PENDING / ANSWERED / 미지정=전체)
    @Operation(summary = "전체 문의 목록 조회(관리자)", description = """
            전체 문의를 페이징으로 조회합니다. status 필터는 선택(미지정 시 전체).
            - 인가: ADMIN""")
    @Parameter(name = "status", description = "상태 필터(PENDING/ANSWERED, 미지정 시 전체)", example = "PENDING")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QnaSummaryResponse>>> getAdminQnas(
            @RequestParam(name = "status", required = false) QnaStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        Pageable safePageable = PageableSanitizer.sanitize(pageable,
                Sort.by(Sort.Direction.DESC, "createdAt"), "createdAt", "answeredAt");
        return ResponseEntity.ok(ApiResponse.success(qnaService.getAdminQnas(status, safePageable)));
    }

    // 2. 문의 상세 조회
    @Operation(summary = "문의 상세 조회(관리자)", description = """
            문의 단건을 코멘트 스레드와 함께 조회합니다. 소유권 검증 없음(URL 레벨 권한 확인).
            - 인가: ADMIN""")
    @Parameter(name = "qnaId", description = "문의 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문의 없음(QNA_001)")
    })
    @GetMapping("/{qnaId}")
    public ResponseEntity<ApiResponse<QnaDetailResponse>> getAdminQna(@PathVariable("qnaId") Long qnaId) {
        return ResponseEntity.ok(ApiResponse.success(qnaService.getAdminQna(qnaId)));
    }

    // 3. 답변 코멘트 추가 (→ 상태 ANSWERED)
    @Operation(summary = "답변 코멘트 추가(관리자)", description = """
            관리자 답변 코멘트를 추가합니다. 추가 시 문의 상태가 ANSWERED로 전이됩니다.
            - 인가: ADMIN""")
    @Parameter(name = "qnaId", description = "문의 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문의 없음(QNA_001)")
    })
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