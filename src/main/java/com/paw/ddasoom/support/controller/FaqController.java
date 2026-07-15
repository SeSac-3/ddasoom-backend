package com.paw.ddasoom.support.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.support.dto.response.FaqResponse;
import com.paw.ddasoom.support.dto.response.FaqSummaryResponse;
import com.paw.ddasoom.support.service.FaqService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

  private final FaqService faqService;

  // 1. 사용자용 FAQ 목록 조회
  @GetMapping
  public ResponseEntity<ApiResponse<List<FaqSummaryResponse>>> getFaqs() {
    return ResponseEntity.ok(ApiResponse.success(faqService.getFaqs()));
  }

  // 2. 사용자용 FAQ 상세 조회
  @GetMapping("/{faqId}")
  public ResponseEntity<ApiResponse<FaqResponse>> getFaq(@PathVariable Long faqId) {
    return ResponseEntity.ok(ApiResponse.success(faqService.getFaq(faqId)));
  }
  
}
