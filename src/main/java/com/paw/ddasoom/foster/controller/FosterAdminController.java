package com.paw.ddasoom.foster.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.foster.dto.request.FosterAdminUpdateRequest;
import com.paw.ddasoom.foster.dto.response.FosterAdminDetailResponse;
import com.paw.ddasoom.foster.dto.response.FosterAdminListResponse;
import com.paw.ddasoom.foster.service.FosterAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/fosters")
public class FosterAdminController {

  /** 관리자 임시보호신청 조회(디테일) */
  @GetMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<FosterAdminDetailResponse>> getFosterDetail(
    @PathVariable Long fosterId){
      FosterAdminDetailResponse response = fosterAdminService.getFosterDetail(fosterId);

      return ResponseEntity.ok(ApiResponse.success(response));
    }
  /** 관리자 임시보호신청 조회(리스트) */
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<FosterAdminListResponse>>> getFosterList(
      @RequestParam(required = false) FosterStatus status,
      @RequestParam(defaultValue = "false") boolean activeOnly,
      @RequestParam(defaultValue = "false") boolean includeDeleted,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ){
    PageResponse<FosterAdminListResponse> response = fosterAdminService.getFosterList(
      status,
      activeOnly,
      includeDeleted,
      startDate,
      endDate,
      PageRequest.of(page, size));

    return ResponseEntity.ok(ApiResponse.success(response));
  }
  
  /** 관리자 임시보호신청 수정 */
  @PatchMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<Void>> update(
    @AuthenticationPrincipal CustomUserDetails userDetails,
    @PathVariable Long fosterId,
    @Valid @RequestBody FosterAdminUpdateRequest request) {
      fosterAdminService.updateFoster(userDetails.getMemberId(), fosterId, request);
      return ResponseEntity.ok(ApiResponse.success("임시보호 신청 처리 정보가 수정되었습니다."));
    }

}
