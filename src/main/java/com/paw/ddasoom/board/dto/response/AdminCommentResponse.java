package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.dto.projection.AdminCommentListProjection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 댓글 목록 행 응답.
 *
 * <p>사용자용 {@link CommentResponse}와 달리 {@code deletedAt}을 포함한다 —
 * 관리자 화면은 삭제된 댓글도 노출하고 "삭제됨" 뱃지로 구분하기 위함.
 */
@Getter
@Builder
public class AdminCommentResponse {

    private final Long commentId;
    private final AuthorResponse author;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;   // null = 활성, 값 있음 = 삭제됨

    public static AdminCommentResponse from(AdminCommentListProjection projection) {
        return AdminCommentResponse.builder()
                .commentId(projection.getCommentId())
                .author(AuthorResponse.from(projection.getMemberId(), projection.getNickname()))
                .content(projection.getContent())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .deletedAt(projection.getDeletedAt())
                .build();
    }
}