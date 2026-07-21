package com.paw.ddasoom.support.controller;

import com.paw.ddasoom.support.service.NoticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
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
import com.paw.ddasoom.support.dto.request.NoticeCreateRequest;
import com.paw.ddasoom.support.dto.request.NoticePinReorderRequest;
import com.paw.ddasoom.support.dto.request.NoticeUpdateRequest;
import com.paw.ddasoom.support.dto.response.NoticeResponse;
import com.paw.ddasoom.support.dto.response.NoticeSummaryResponse;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    // 1. 관리자용 공지사항 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NoticeSummaryResponse>>> getAdminNotices(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getAdminNotices(pageable)));
    }

    // 2. 공지사항 상세 조회
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeResponse>> getAdminNotice(@PathVariable("noticeId") Long noticeId) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.getAdminNotice(noticeId)));
    }

  // 3. 공지사항 등록
    @PostMapping
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NoticeCreateRequest request) {
        NoticeResponse response = noticeService.createNotice(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("공지사항이 등록되었습니다.", response));
    }

    // 4. 공지사항 전체 수정
    @PutMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @PathVariable("noticeId") Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.updateNotice(noticeId, request)));
    }

    // 5. 공지사항 노출 여부
    @PatchMapping("/{noticeId}/visibility")
    public ResponseEntity<ApiResponse<Void>> changeVisibility(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam("isVisible") boolean isVisible) {
        noticeService.changeVisibility(noticeId, isVisible);
        return ResponseEntity.ok(ApiResponse.success("노출 여부가 변경되었습니다."));
    }

    // 6. 상단 고정 순서 재정렬
    @PatchMapping("/pin-order")
    public ResponseEntity<ApiResponse<Void>> reorderPinned(
            @Valid @RequestBody NoticePinReorderRequest request) {
        noticeService.reorderPinned(request.getNoticeIds());
        return ResponseEntity.ok(ApiResponse.success("고정 순서가 변경되었습니다."));
    }

    // 7. 공지사항 삭제
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable("noticeId") Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 삭제되었습니다."));
    }
}
