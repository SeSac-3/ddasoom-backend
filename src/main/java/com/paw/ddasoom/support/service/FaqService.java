package com.paw.ddasoom.support.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.common.util.HtmlSanitizer;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.image.service.ImageService;
import com.paw.ddasoom.support.domain.Faq;
import com.paw.ddasoom.support.dto.request.FaqCreateRequest;
import com.paw.ddasoom.support.dto.request.FaqUpdateRequest;
import com.paw.ddasoom.support.dto.response.FaqResponse;
import com.paw.ddasoom.support.dto.response.FaqSummaryResponse;
import com.paw.ddasoom.support.exception.SupportErrorCode;
import com.paw.ddasoom.support.exception.SupportException;
import com.paw.ddasoom.support.repository.FaqRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FaqService {

// ================================
// FAQ 로직 서비스
// ================================
  
  private final FaqRepository faqRepository;
  private final ImageService imageService;

// ====== 1. 유저용(조회) ========

  // 1) 전체 FAQ 목록 조회
  @Transactional(readOnly = true)
  public List<FaqSummaryResponse> getFaqs() {
      return faqRepository.findAllForUser().stream()
              .map(FaqSummaryResponse::from)
              .toList();
  }

  // 2) FAQ 단건 상세 내용 조회
  @Transactional(readOnly = true)
  public FaqResponse getFaq(Long faqId) {
    Faq faq = getFaqEntity(faqId);

    // 비노출 FAQ = 유저기준 "삭제"와 동일 취급
    if (!faq.getIsVisible()) {
      throw new SupportException(SupportErrorCode.FAQ_NOT_FOUND);
    }
    List<ImageResponse> images = imageService.getImages(OwnerType.FAQ, faqId);
    return FaqResponse.from(faq, images);
  }

// ====== 2. 관리자용(CURD) ========

  // 1) FAQ 전체 목록 조회 (비노출 포함)
  @Transactional (readOnly = true)
  public List<FaqSummaryResponse> getAdminFaqs() {
    return faqRepository.findAllForAdmin().stream()
      .map(FaqSummaryResponse::from)
      .toList();
  }

  // 2) FAQ 상세 조회
  @Transactional (readOnly = true)
  public FaqResponse getAdminFaq(Long faqId) {
    Faq faq = getFaqEntity(faqId);
    List<ImageResponse> images = imageService.getImages(OwnerType.FAQ, faqId);
    return FaqResponse.from(faq, images);
  }

  // 3) 새로운 FAQ 등록
  @Transactional
  public FaqResponse createFaq(FaqCreateRequest request) {
    Faq faq = Faq.builder()
      .category(request.getCategory())
      .question(request.getQuestion())
      .answer(HtmlSanitizer.sanitize(request.getAnswer()))
      .build();
    Faq savedFaq = faqRepository.save(faq);
    imageService.attach(request.getImageIds(), OwnerType.FAQ, savedFaq.getId());
    List<ImageResponse> images = imageService.getImages(OwnerType.FAQ, savedFaq.getId());
    return FaqResponse.from(savedFaq, images);
  }

  // 4) FAQ 수정
  @Transactional
  public FaqResponse updateFaq(Long faqId, FaqUpdateRequest request) {
    Faq faq = getFaqEntity(faqId);
    faq.update(request.getCategory(), request.getQuestion(), HtmlSanitizer.sanitize(request.getAnswer()));
    imageService.syncImages(request.getImageIds(), OwnerType.FAQ, faqId);
    faqRepository.flush();
    List<ImageResponse> images = imageService.getImages(OwnerType.FAQ, faqId);
    return FaqResponse.from(faq, images);
  }

  /**
  * 5) FAQ 노출 여부 변경
  * @param isVisible 사용자가 FAQ를 볼 수 있게 할지 여부 (true: 노출, false: 숨김)
  */
  @Transactional
  public void changeVisibility(Long faqId, boolean isVisible) {
    Faq faq = getFaqEntity(faqId);
    faq.changeVisibility(isVisible);
  }

  // 6) FAQ 삭제 (논리 삭제)
  @Transactional
  public void deleteFaq(Long faqId) {
    Faq faq = getFaqEntity(faqId);
    faq.softDelete();
    imageService.syncImages(List.of(), OwnerType.FAQ, faqId);
  }

// ====== 3. 내부 조회 ========

  // 1) Faq 단건 조회 공통 내부 메서드 (논리삭제X 데이터만 조회)
  private Faq getFaqEntity(Long faqId) {
    return faqRepository.findByIdAndDeletedAtIsNull(faqId)
      .orElseThrow(() -> new SupportException(SupportErrorCode.FAQ_NOT_FOUND));
  }
}

