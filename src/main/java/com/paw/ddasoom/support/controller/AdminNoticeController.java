package com.paw.ddasoom.support.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.common.util.PageableSanitizer;
import com.paw.ddasoom.support.dto.request.NoticeCreateRequest;
import com.paw.ddasoom.support.dto.request.NoticePinReorderRequest;
import com.paw.ddasoom.support.dto.request.NoticeUpdateRequest;
import com.paw.ddasoom.support.dto.response.NoticeResponse;
import com.paw.ddasoom.support.dto.response.NoticeSummaryResponse;
import com.paw.ddasoom.support.service.NoticeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@Tag(name = "관리자 공지사항(Admin Notice)", description = "관리자 — 공지사항 목록·상세·등록·수정·노출·고정순서·삭제 API")
@SecurityRequirement(name = "bearerAuth")   // /api/admin 하위 = ADMIN 전용
public class AdminNoticeController {

    private final NoticeService noticeService;

    // 1. 관리자용 공지사항 목록 조회
    @Operation(summary = "공지사항 목록 조회(관리자)", description = """
            비노출·삭제 포함 여부는 서비스 정책에 따르며, 페이징으로 조회합니다.
            - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NoticeSummaryResponse>>> getAdminNotices(
            @PageableDefault(size = 10) Pageable pageable) {
        Pageable safePageable = PageableSanitizer.sanitize(pageable,
                Sort.by(Sort.Direction.DESC, "createdAt"), "createdAt", "pinOrder");
        return ResponseEntity.ok(ApiResponse.success(noticeService.getAdminNotices(safePageable)));
    }

    // 2. 공지사항 상세 조회
    @Operation(summary = "공지사항 상세 조회(관리자)", description = """
            공지사항 단건을 조회합니다.
            - 인가: ADMIN""")
    @Parameter(name = "noticeId", description = "공지 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지 없음(SUPPORT_001)")
    })
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeResponse>> getAdminNotice(@PathVariable("noticeId") Long noticeId) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getAdminNotice(noticeId)));
    }

    // 3. 공지사항 등록
    @Operation(summary = "공지사항 등록", description = """
            새 공지사항을 등록합니다. 본문 이미지(imageIds)는 순서대로 연결됩니다.
            - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NoticeCreateRequest request) {
        NoticeResponse response = noticeService.createNotice(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항이 등록되었습니다.", response));
    }

    // 4. 공지사항 전체 수정
    @Operation(summary = "공지사항 전체 수정", description = """
            공지사항을 전체 치환(PUT)합니다. 이미지 목록은 최종 상태로 동기화됩니다.
            - 인가: ADMIN""")
    @Parameter(name = "noticeId", description = "공지 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지 없음(SUPPORT_001)")
    })
    @PutMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("noticeId") Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                noticeService.updateNotice(userDetails.getMemberId(), noticeId, request)));
    }

    // 5. 공지사항 노출 여부
    @Operation(summary = "공지사항 노출 여부 변경", description = """
            공지사항의 노출/숨김 상태를 변경합니다.
            - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지 없음(SUPPORT_001)")
    })
    @PatchMapping("/{noticeId}/visibility")
    public ResponseEntity<ApiResponse<Void>> changeVisibility(
            @Parameter(name = "noticeId", description = "공지 PK", required = true, example = "1")
            @PathVariable("noticeId") Long noticeId,
            @Parameter(name = "isVisible", description = "노출 여부(true=노출, false=숨김)", required = true, example = "true")
            @RequestParam("isVisible") boolean isVisible) {
        noticeService.changeVisibility(noticeId, isVisible);
        return ResponseEntity.ok(ApiResponse.success("노출 여부가 변경되었습니다."));
    }

    // 6. 상단 고정 순서 재정렬
    @Operation(summary = "상단 고정 순서 재정렬", description = """
            상단 고정 공지의 순서를 재정렬합니다. 빈 리스트 전송 시 전체 고정 해제.
            - 인가: ADMIN""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "고정 순서에 중복 공지 포함(SUPPORT_003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지 없음(SUPPORT_001)")
    })
    @PatchMapping("/pin-order")
    public ResponseEntity<ApiResponse<Void>> reorderPinned(
            @Valid @RequestBody NoticePinReorderRequest request) {
        noticeService.reorderPinned(request.getNoticeIds());
        return ResponseEntity.ok(ApiResponse.success("고정 순서가 변경되었습니다."));
    }

    // 7. 공지사항 삭제
    @Operation(summary = "공지사항 삭제", description = """
            공지사항을 삭제합니다(soft delete). 연결 이미지도 함께 정리됩니다.
            - 인가: ADMIN""")
    @Parameter(name = "noticeId", description = "공지 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지 없음(SUPPORT_001)")
    })
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable("noticeId") Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 삭제되었습니다."));
    }
}