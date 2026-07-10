package com.paw.ddasoom.foster.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.foster.dto.request.FosterCreateRequest;
import com.paw.ddasoom.foster.service.FosterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fosters")
public class FosterController {

  private final FosterService fosterService;
  // private final FosterAdminService fosterAdminService;

  /** 유저 임시보호신청 작성 */
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody FosterCreateRequest request) {

    fosterService.create(userDetails.getMemberId(), request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("임시보호 신청이 완료되었습니다."));
  }

}
