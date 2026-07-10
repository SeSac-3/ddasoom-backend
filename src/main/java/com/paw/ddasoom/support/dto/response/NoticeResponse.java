package com.paw.ddasoom.support.dto.response;

import java.time.LocalDateTime;

import com.paw.ddasoom.support.domain.Notice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeResponse {

    private Long noticeId;
    private String writerNickname;
    private String title;
    private String content;
    private Boolean isVisible;
    private Integer pinOrder; // 순서 정보용
    private Boolean isPinned; // 고정 상태 정보용
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .noticeId(notice.getId())
                .writerNickname(notice.getMember().getNickname())
                .title(notice.getTitle())
                .content(notice.getContent())
                .isVisible(notice.getIsVisible())
                .pinOrder(notice.getPinOrder())
                .isPinned(notice.isPinned())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}