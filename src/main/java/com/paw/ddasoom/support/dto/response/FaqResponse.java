package com.paw.ddasoom.support.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.support.domain.Faq;
import com.paw.ddasoom.support.domain.FaqCategory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaqResponse {

  private Long faqId;
  private FaqCategory category;
  private String question;
  private String answer;
  private Boolean isVisible;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
   private List<ImageResponse> images;

  public static FaqResponse from(Faq faq,  List<ImageResponse> images) {
    return FaqResponse.builder()
      .faqId(faq.getId())
      .category(faq.getCategory())
      .question(faq.getQuestion())
      .answer(faq.getAnswer())
      .isVisible(faq.getIsVisible())
      .images(images)
      .createdAt(faq.getCreatedAt())
      .updatedAt(faq.getUpdatedAt())
      .build();
  }
}
