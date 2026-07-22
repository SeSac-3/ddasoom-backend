package com.paw.ddasoom.support.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.util.PageableSanitizer;
import com.paw.ddasoom.support.dto.response.NoticeResponse;
import com.paw.ddasoom.support.dto.response.NoticeSummaryResponse;
import com.paw.ddasoom.support.service.NoticeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(name = "공지사항(Notice)", description = "사용자 — 공지사항 목록·상세 조회 API (공개)")
public class NoticeController {

    private final NoticeService noticeService;

    // 1. 사용자용 공지사항 목록 조회 (노출 + 미삭제, 고정글 우선)
    @Operation(summary = "공지사항 목록 조회", description = """
            노출 중인 공지사항을 페이징으로 조회합니다. 상단 고정글(pin)이 우선 정렬됩니다.
            - 인가: 공개(비로그인 가능)""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NoticeSummaryResponse>>> getNotices(
           @PageableDefault(size = 10) Pageable pageable) {
        // pinOrder = 상단 고정 순서 — 공지 목록의 유효한 정렬 기준이라 허용
        Pageable safePageable = PageableSanitizer.sanitize(pageable,
                Sort.by(Sort.Direction.DESC, "createdAt"), "createdAt", "pinOrder");
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotices(safePageable)));
    }

    // 2. 사용자용 공지사항 상세 조회 (비노출 공지는 서비스에서 404 처리)
    @Operation(summary = "공지사항 상세 조회", description = """
            공지사항 단건을 조회합니다. 비노출 공지는 404로 처리됩니다.
            - 인가: 공개(비로그인 가능)""")
    @Parameter(name = "noticeId", description = "공지 PK", required = true, example = "1")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지 없음/비노출(SUPPORT_001)")
    })
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeResponse>> getNotice(@PathVariable("noticeId") Long noticeId) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getNotice(noticeId)));
    }
}
