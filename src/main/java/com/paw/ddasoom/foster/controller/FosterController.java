package com.paw.ddasoom.foster.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.foster.dto.request.FosterCreateRequest;
import com.paw.ddasoom.foster.dto.request.FosterUpdateRequest;
import com.paw.ddasoom.foster.dto.response.FosterUserDetailResponse;
import com.paw.ddasoom.foster.dto.response.FosterUserListResponse;
import com.paw.ddasoom.foster.service.FosterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fosters")
public class FosterController {

  private final FosterService fosterService;

  /** 유저 임시보호신청 작성 */
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody FosterCreateRequest request) {
    fosterService.create(userDetails.getMemberId(), request);
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("임시보호 신청이 완료되었습니다."));
  }

  /** 유저 임시보호신청 수정 */
  @PatchMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<Void>> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long fosterId,
      @Valid @RequestBody FosterUpdateRequest request) {

    fosterService.update(userDetails.getMemberId(), fosterId, request);

    return ResponseEntity.ok(ApiResponse.success("임시보호 신청이 수정되었습니다."));
  }

  /** 유저 임시보호신청 조회(리스트) */
  @GetMapping("/my")
  public ResponseEntity<ApiResponse<PageResponse<FosterUserListResponse>>> getFosterList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(required = false) FosterStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PageResponse<FosterUserListResponse> response = fosterService.getFosterList(
      userDetails.getMemberId(),
      status,
      PageRequest.of(page, size));

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /** 유저 임시보호신청 조회(디테일) */
  @GetMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<FosterUserDetailResponse>> getFosterDetail(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long fosterId) {
    FosterUserDetailResponse response = fosterService.getFosterDetail(userDetails.getMemberId(), fosterId);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /** 유저 임시보호신청 삭제 */
  @DeleteMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long fosterId) {
    fosterService.delete(userDetails.getMemberId(), fosterId);

    return ResponseEntity.ok(ApiResponse.success("임시보호 신청이 삭제되었습니다."));
  }
}
