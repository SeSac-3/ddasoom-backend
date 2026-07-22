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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
@Tag(name = "자주 묻는 질문(FAQ)", description = "사용자 — FAQ 목록·상세·카테고리 조회 API (공개)")
public class FaqController {

  private final FaqService faqService;

  // 카테고리는 enum 고정값이라 요청마다 스트림을 다시 돌릴 이유가 없음
  // 서버 기동 시 1회 생성, 재사용
  private static final List<FaqCategoryResponse> CACHED_CATEGORIES =
      Arrays.stream(FaqCategory.values())
        .map(FaqCategoryResponse::from)
        .toList();

  // 1. 사용자용 FAQ 목록 조회
  @Operation(summary = "FAQ 목록 조회", description = """
          노출 중인 FAQ 전체 목록을 조회합니다.
          - 인가: 공개(비로그인 가능)""")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<List<FaqSummaryResponse>>> getFaqs() {
    return ResponseEntity.ok(ApiResponse.success(faqService.getFaqs()));
  }

  // 2. 사용자용 FAQ 상세 조회
  @Operation(summary = "FAQ 상세 조회", description = """
          FAQ 단건을 조회합니다. 비노출 FAQ는 404로 처리됩니다.
          - 인가: 공개(비로그인 가능)""")
  @Parameter(name = "faqId", description = "FAQ PK", required = true, example = "1")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FAQ 없음/비노출(SUPPORT_002)")
  })
  @GetMapping("/{faqId}")
  public ResponseEntity<ApiResponse<FaqResponse>> getFaq(@PathVariable("faqId") Long faqId) {
    return ResponseEntity.ok(ApiResponse.success(faqService.getFaq(faqId)));
  }

  // 3. FAQ 카테고리 목록 조회 (캐시된 리스트 반환)
  @Operation(summary = "FAQ 카테고리 목록 조회", description = """
          FAQ 카테고리(enum 고정값) 목록을 조회합니다. 서버 기동 시 1회 생성된 캐시를 반환합니다.
          - 인가: 공개(비로그인 가능)""")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
  })
  @GetMapping("/categories")
  public ResponseEntity<ApiResponse<List<FaqCategoryResponse>>> getFaqCategories() {
      return ResponseEntity.ok(ApiResponse.success(CACHED_CATEGORIES));
  }
}
