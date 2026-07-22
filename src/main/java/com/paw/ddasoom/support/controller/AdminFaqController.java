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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
@Tag(name = "관리자 FAQ(Admin FAQ)", description = "관리자 — FAQ 목록·상세·등록·수정·노출·삭제 API")
@SecurityRequirement(name = "bearerAuth")   // /api/admin 하위 = ADMIN 전용
public class AdminFaqController {

  private final FaqService faqService;

  // 1. 관리자용 FAQ 목록 조회
  @Operation(summary = "FAQ 목록 조회(관리자)", description = """
          비노출 포함 FAQ 전체 목록을 조회합니다.
          - 인가: ADMIN""")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
  })
  @GetMapping
  public ResponseEntity<ApiResponse<List<FaqSummaryResponse>>> getAdminFaqs() {
    return ResponseEntity.ok(ApiResponse.success(faqService.getAdminFaqs()));
  }

  // 2. FAQ 상세 조회
  @Operation(summary = "FAQ 상세 조회(관리자)", description = """
          FAQ 단건을 조회합니다.
          - 인가: ADMIN""")
  @Parameter(name = "faqId", description = "FAQ PK", required = true, example = "1")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FAQ 없음(SUPPORT_002)")
  })
  @GetMapping("/{faqId}")
  public ResponseEntity<ApiResponse<FaqResponse>> getAdminFaq(@PathVariable("faqId") Long faqId) {
    return ResponseEntity.ok(ApiResponse.success(faqService.getAdminFaq(faqId)));
  }

  // 3. FAQ 등록
  @Operation(summary = "FAQ 등록", description = """
          새 FAQ를 등록합니다. 본문 이미지(imageIds)는 순서대로 연결됩니다.
          - 인가: ADMIN""")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님")
  })
  @PostMapping
  public ResponseEntity<ApiResponse<FaqResponse>> createFaq(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody FaqCreateRequest request) {
    FaqResponse response = faqService.createFaq(userDetails.getMemberId(), request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("FAQ가 등록되었습니다.", response));
  }

  // 4. FAQ 전체 수정
  @Operation(summary = "FAQ 전체 수정", description = """
          FAQ를 전체 치환(PUT)합니다. 이미지 목록은 최종 상태로 동기화됩니다.
          - 인가: ADMIN""")
  @Parameter(name = "faqId", description = "FAQ PK", required = true, example = "1")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 유효성 오류"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FAQ 없음(SUPPORT_002)")
  })
  @PutMapping("/{faqId}")
  public ResponseEntity<ApiResponse<FaqResponse>> updateFaq(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @PathVariable("faqId") Long faqId,
          @Valid @RequestBody FaqUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
            faqService.updateFaq(userDetails.getMemberId(), faqId, request)));
  }

  // 5. FAQ 노출 여부 변경
  @Operation(summary = "FAQ 노출 여부 변경", description = """
          FAQ의 노출/숨김 상태를 변경합니다.
          - 인가: ADMIN""")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FAQ 없음(SUPPORT_002)")
  })
  @PatchMapping("/{faqId}/visibility")
  public ResponseEntity<ApiResponse<Void>> changeVisibility(
          @Parameter(name = "faqId", description = "FAQ PK", required = true, example = "1")
          @PathVariable("faqId") Long faqId,
          @Parameter(name = "isVisible", description = "노출 여부(true=노출, false=숨김)", required = true, example = "true")
          @RequestParam("isVisible") boolean isVisible) {
    faqService.changeVisibility(faqId, isVisible);
    return ResponseEntity.ok(ApiResponse.success("노출 여부가 변경되었습니다."));
  }

  // 6. FAQ 삭제
  @Operation(summary = "FAQ 삭제", description = """
          FAQ를 삭제합니다(soft delete). 연결 이미지도 함께 정리됩니다.
          - 인가: ADMIN""")
  @Parameter(name = "faqId", description = "FAQ PK", required = true, example = "1")
  @ApiResponses({
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 아님"),
          @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FAQ 없음(SUPPORT_002)")
  })
  @DeleteMapping("/{faqId}")
  public ResponseEntity<ApiResponse<Void>> deleteFaq(@PathVariable("faqId") Long faqId) {
    faqService.deleteFaq(faqId);
    return ResponseEntity.ok(ApiResponse.success("FAQ가 삭제되었습니다."));
  }

}