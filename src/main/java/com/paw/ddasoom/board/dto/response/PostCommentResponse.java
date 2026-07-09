package com.paw.ddasoom.board.dto.response;

import com.paw.ddasoom.board.domain.PostComment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostCommentResponse {

    private final Long commentId;
    private final Long postId;
    private final AuthorResponse author;

    /** NULL = 일반 댓글, 값 있음 = 대댓글 (부모 댓글 ID) */
    private final Long parentCommentId;

    private final String content;
    private final LocalDateTime createdAt;

    @Builder
    private PostCommentResponse(Long commentId, Long postId, AuthorResponse author,
                                Long parentCommentId, String content, LocalDateTime createdAt) {
        this.commentId = commentId;
        this.postId = postId;
        this.author = author;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static PostCommentResponse from(PostComment comment) {
        return PostCommentResponse.builder()
                .commentId(comment.getId())
                .postId(comment.getPost().getId())
                .author(AuthorResponse.from(comment.getMember()))
                .parentCommentId(comment.isReply() ? comment.getParentComment().getId() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}