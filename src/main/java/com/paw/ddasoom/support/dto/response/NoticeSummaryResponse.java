package com.paw.ddasoom.support.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.support.domain.Notice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeSummaryResponse {

  private Long noticeId;
  private String title;
  private Boolean isVisible;
  private Boolean isPinned;
  private LocalDateTime createdAt;

  public static NoticeSummaryResponse from(Notice notice) {
    return NoticeSummaryResponse.builder()
    .noticeId(notice.getId())
    .title(notice.getTitle())
    .isVisible(notice.getIsVisible())
    .isPinned(notice.isPinned())
    .createdAt(notice.getCreatedAt())
    .build();
  }


}
