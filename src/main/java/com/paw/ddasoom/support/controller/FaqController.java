package com.paw.ddasoom.support.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.support.domain.FaqCategory;
import com.paw.ddasoom.support.dto.response.FaqCategoryResponse;
import com.paw.ddasoom.support.dto.response.FaqResponse;
import com.paw.ddasoom.support.dto.response.FaqSummaryResponse;
import com.paw.ddasoom.support.service.FaqService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

  private final FaqService faqService;

  // 카테고리는 enum 고정값이라 요청마다 스트림을 다시 돌릴 이유가 없음
  // 서버 기동 시 1회 생성, 재사용
  private static final List<FaqCategoryResponse> CACHED_CATEGORIES =
      Arrays.stream(FaqCategory.values())
        .map(FaqCategoryResponse::from)
        .toList();

  // 1. 사용자용 FAQ 목록 조회
  @GetMapping
  public ResponseEntity<ApiResponse<List<FaqSummaryResponse>>> getFaqs() {
    return ResponseEntity.ok(ApiResponse.success(faqService.getFaqs()));
  }

  // 2. 사용자용 FAQ 상세 조회
  @GetMapping("/{faqId}")
  public ResponseEntity<ApiResponse<FaqResponse>> getFaq(@PathVariable("faqId") Long faqId) {
    return ResponseEntity.ok(ApiResponse.success(faqService.getFaq(faqId)));
  }

  // 3. FAQ 카테고리 목록 조회 (캐시된 리스트 반환)
  @GetMapping("/categories")
  public ResponseEntity<ApiResponse<List<FaqCategoryResponse>>> getFaqCategories() {
      return ResponseEntity.ok(ApiResponse.success(CACHED_CATEGORIES));
  }
}
