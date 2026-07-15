package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.PostComment;
import com.paw.ddasoom.board.dto.projection.CommentListProjection;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private final Long commentId;
    private final AuthorResponse author;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /** 생성/수정 경로용 — 엔티티에서 직접 변환 */
    public static CommentResponse from(PostComment comment) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .author(AuthorResponse.from(comment.getMember()))
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /** 목록(projection) 경로용 */
    public static CommentResponse from(CommentListProjection projection) {
        return CommentResponse.builder()
                .commentId(projection.getCommentId())
                .author(AuthorResponse.from(projection.getMemberId(), projection.getNickname()))
                .content(projection.getContent())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .build();
    }
}