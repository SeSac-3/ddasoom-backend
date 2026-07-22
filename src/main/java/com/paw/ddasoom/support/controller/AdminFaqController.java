package com.paw.ddasoom.support.controller;

import java.util.List;

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
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.support.dto.request.FaqCreateRequest;
import com.paw.ddasoom.support.dto.request.FaqUpdateRequest;
import com.paw.ddasoom.support.dto.response.FaqResponse;
import com.paw.ddasoom.support.dto.response.FaqSummaryResponse;
import com.paw.ddasoom.support.service.FaqService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
public class AdminFaqController {

  private final FaqService faqService;

  // 1. 관리자용 FAQ 목록 조회
  @GetMapping
  public ResponseEntity<ApiResponse<List<FaqSummaryResponse>>> getAdminFaqs() {
    return ResponseEntity.ok(ApiResponse.success(faqService.getAdminFaqs()));
  }

  // 2. FAQ 상세 조회
  @GetMapping("/{faqId}")
  public ResponseEntity<ApiResponse<FaqResponse>> getAdminFaq(@PathVariable("faqId") Long faqId) {
    return ResponseEntity.ok(ApiResponse.success(faqService.getAdminFaq(faqId)));
  }

  // 3. FAQ 등록
  @PostMapping
  public ResponseEntity<ApiResponse<FaqResponse>> createFaq(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody FaqCreateRequest request) {
    FaqResponse response = faqService.createFaq(userDetails.getMemberId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("FAQ가 등록되었습니다.", response));
  }

  // 4. FAQ 전체 수정
  @PutMapping("/{faqId}")
  public ResponseEntity<ApiResponse<FaqResponse>> updateFaq(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @PathVariable("faqId") Long faqId,
          @Valid @RequestBody FaqUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
            faqService.updateFaq(userDetails.getMemberId(), faqId, request)));
  }

  // 5. FAQ 노출 여부 변경
  @PatchMapping("/{faqId}/visibility")
  public ResponseEntity<ApiResponse<Void>> changeVisibility(
          @PathVariable("faqId") Long faqId,
          @RequestParam("isVisible") boolean isVisible) {
    faqService.changeVisibility(faqId, isVisible);
    return ResponseEntity.ok(ApiResponse.success("노출 여부가 변경되었습니다."));
  }

  // 6. FAQ 삭제
  @DeleteMapping("/{faqId}")
  public ResponseEntity<ApiResponse<Void>> deleteFaq(@PathVariable("faqId") Long faqId) {
    faqService.deleteFaq(faqId);
    return ResponseEntity.ok(ApiResponse.success("FAQ가 삭제되었습니다."));
  }

}